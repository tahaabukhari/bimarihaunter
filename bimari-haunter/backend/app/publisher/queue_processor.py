"""
Background delivery-queue processor.

Runs as an ``asyncio`` background task inside the API service.
Every 5 seconds it drains queued / retryable items from the
``delivery_queue`` table and pushes them to connected clients.
"""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone

import structlog
from sqlalchemy import select, or_

from app.database.engine import async_session
from app.database.models import (
    AppSession,
    DeliveryQueue,
    ReportV1Insight,
    ReportV2Full,
)
from app.publisher.formatter import format_v1_payload, format_v2_payload
from app.publisher.realtime import publisher

logger = structlog.get_logger(__name__)


async def run_delivery_queue() -> None:
    """Infinite loop that processes the delivery_queue every 5 s."""
    logger.info("delivery_queue_processor_started")

    while True:
        try:
            await _process_batch()
        except Exception as exc:
            logger.error("delivery_queue_error", error=str(exc))

        await asyncio.sleep(5)


async def _process_batch() -> None:
    """Process a single batch of queued delivery items."""
    async with async_session() as db:
        stmt = (
            select(DeliveryQueue)
            .where(
                or_(
                    DeliveryQueue.status == "queued",
                    DeliveryQueue.status == "failed",
                ),
                DeliveryQueue.retry_count < publisher.max_retry,
            )
            .order_by(DeliveryQueue.priority, DeliveryQueue.created_at)
            .limit(publisher.delivery_batch_size)
        )

        result = await db.execute(stmt)
        items = result.scalars().all()

        if not items:
            return

        for item in items:
            try:
                session = await db.get(AppSession, item.session_id)
                if not session:
                    item.status = "failed"
                    item.error_message = "session_not_found"
                    await db.commit()
                    continue

                # Determine report type and fetch
                payload = None
                report = None

                if item.report_v1_id:
                    report = await db.get(ReportV1Insight, item.report_v1_id)
                    if report:
                        payload = format_v1_payload(report)
                elif item.report_v2_id:
                    report = await db.get(ReportV2Full, item.report_v2_id)
                    if report:
                        payload = format_v2_payload(report)

                if not payload or not report:
                    item.status = "failed"
                    item.error_message = "report_not_found"
                    await db.commit()
                    continue

                # Check if client is connected
                ws = publisher.active_connections.get(session.device_id)
                if ws is not None:
                    try:
                        await ws.send_json(payload)
                        item.status = "delivered"
                        item.sent_at = datetime.now(timezone.utc)
                        item.delivered_at = datetime.now(timezone.utc)

                        # Update report delivery status
                        report.delivery_status = "delivered"
                        report.delivered_at = datetime.now(timezone.utc)

                        logger.info(
                            "queue_item_delivered",
                            item_id=item.id,
                            device_id=session.device_id,
                        )
                    except Exception as exc:
                        item.retry_count += 1
                        item.status = "failed"
                        item.error_message = str(exc)[:500]
                        logger.warning(
                            "queue_push_failed",
                            item_id=item.id,
                            error=str(exc),
                        )
                else:
                    # Session offline – keep queued for later
                    item.status = "queued"

                await db.commit()

            except Exception as exc:
                logger.error(
                    "queue_item_processing_error",
                    item_id=item.id,
                    error=str(exc),
                )
                await db.rollback()
