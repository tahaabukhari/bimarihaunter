"""
Realtime publisher – manages WebSocket connections and pushes V1/V2
payloads to Android clients.

The publisher maintains an ``active_connections`` map of device_id →
WebSocket, matches insights to sessions based on user_preferences,
and provides priority-based delivery queuing.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Optional

import structlog
from fastapi import WebSocket
from sqlalchemy import select, and_, or_, cast, Float
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.engine import async_session
from app.database.models import (
    AppSession,
    DeliveryQueue,
    ReportV1Insight,
    ReportV2Full,
)
from app.publisher.formatter import format_v1_payload, format_v2_payload

logger = structlog.get_logger(__name__)


class RealtimePublisher:
    """Singleton publisher managing live WebSocket delivery."""

    def __init__(self) -> None:
        self.active_connections: dict[str, WebSocket] = {}
        self.delivery_batch_size: int = 50
        self.max_retry: int = 3

    # ── Connection management ───────────────────────────────

    def register(self, device_id: str, ws: WebSocket) -> None:
        self.active_connections[device_id] = ws

    def unregister(self, device_id: str) -> None:
        self.active_connections.pop(device_id, None)

    # ── V1 Insight publishing ───────────────────────────────

    async def publish_insight(self, report_id: int) -> None:
        """Push a V1 insight to all matching sessions."""
        async with async_session() as db:
            insight = await db.get(ReportV1Insight, report_id)
            if not insight:
                logger.warning("insight_not_found", report_id=report_id)
                return

            # Find matching sessions
            sessions = await self._find_matching_sessions(db, insight)

            for session in sessions:
                priority = self.calculate_priority(insight)
                queue_item = DeliveryQueue(
                    session_id=session.id,
                    report_v1_id=insight.id,
                    priority=priority,
                    status="queued",
                )
                db.add(queue_item)

            await db.commit()

            # Immediately push to online clients
            await self.push_to_online_sessions(db, insight, sessions)

    async def _find_matching_sessions(
        self, db: AsyncSession, insight: ReportV1Insight
    ) -> list[AppSession]:
        """Find sessions whose preferences match the insight."""
        stmt = select(AppSession)
        result = await db.execute(stmt)
        all_sessions = result.scalars().all()

        matched: list[AppSession] = []
        severity = float(insight.severity_score)

        for session in all_sessions:
            prefs = session.user_preferences or {}

            # National alert: severity > 0.8 goes to everyone
            if severity > 0.8:
                matched.append(session)
                continue

            # Check location match
            regions = prefs.get("regions", [])
            if regions and insight.primary_location:
                if insight.primary_location not in regions:
                    continue

            # Check disease match
            diseases = prefs.get("diseases", [])
            # (no disease filter = match all)

            # Check severity threshold
            threshold = float(prefs.get("severity_threshold", 0.0))
            if severity < threshold:
                continue

            matched.append(session)

        return matched

    async def push_to_online_sessions(
        self,
        db: AsyncSession,
        insight: ReportV1Insight,
        sessions: list[AppSession],
    ) -> None:
        """Send the V1 payload to all currently connected matching clients."""
        payload = format_v1_payload(insight)

        for session in sessions:
            ws = self.active_connections.get(session.device_id)
            if ws is None:
                continue
            try:
                await ws.send_json(payload)
                insight.delivery_status = "delivered"
                insight.delivered_at = datetime.now(timezone.utc)
                logger.info(
                    "insight_pushed",
                    device_id=session.device_id,
                    report_id=insight.id,
                )
            except Exception as exc:
                logger.error(
                    "insight_push_failed",
                    device_id=session.device_id,
                    error=str(exc),
                )

        await db.commit()

    # ── V2 Full Report publishing ───────────────────────────

    async def publish_full_report(
        self,
        report_id: int,
        *,
        request_session_id: Optional[int] = None,
    ) -> None:
        """Push a V2 report – directly if requested, else queue for batch."""
        async with async_session() as db:
            report = await db.get(ReportV2Full, report_id)
            if not report:
                logger.warning("report_not_found", report_id=report_id)
                return

            if request_session_id:
                session = await db.get(AppSession, request_session_id)
                if session:
                    await self.push_single_report(db, report, session)
            else:
                # Queue for batch delivery
                stmt = select(AppSession)
                result = await db.execute(stmt)
                sessions = result.scalars().all()
                for session in sessions:
                    queue_item = DeliveryQueue(
                        session_id=session.id,
                        report_v2_id=report.id,
                        priority=5,
                        status="queued",
                    )
                    db.add(queue_item)
                await db.commit()

    async def push_single_report(
        self,
        db: AsyncSession,
        report: ReportV2Full,
        session: AppSession,
    ) -> None:
        """Push a single V2 payload to a specific session."""
        ws = self.active_connections.get(session.device_id)
        if ws is None:
            logger.info("session_offline", device_id=session.device_id)
            return

        payload = format_v2_payload(report)
        try:
            await ws.send_json(payload)
            report.delivery_status = "delivered"
            report.delivered_at = datetime.now(timezone.utc)
            await db.commit()
            logger.info(
                "full_report_pushed",
                device_id=session.device_id,
                report_id=report.id,
            )
        except Exception as exc:
            logger.error(
                "full_report_push_failed",
                device_id=session.device_id,
                error=str(exc),
            )

    # ── Priority calculation ────────────────────────────────

    @staticmethod
    def calculate_priority(insight: ReportV1Insight) -> int:
        """Return an integer priority 1 (highest) – 10 (lowest)."""
        priority = 5

        # Urgency level adjustment
        urgency = (insight.urgency_level or "").lower()
        if urgency == "critical":
            priority -= 4
        elif urgency == "high":
            priority -= 2

        # Severity adjustment
        severity = float(insight.severity_score)
        if severity > 0.8:
            priority -= 2
        elif severity > 0.6:
            priority -= 1

        # Freshness adjustment
        if insight.published_at:
            age_hours = (
                datetime.now(timezone.utc) - insight.published_at.replace(
                    tzinfo=timezone.utc
                )
            ).total_seconds() / 3600
            if age_hours < 1:
                priority -= 1

        # Clamp
        return max(1, min(priority, 10))

    # ── Retry helper ────────────────────────────────────────

    async def mark_for_retry(
        self,
        *,
        session_id: int,
        report_v1_id: Optional[int] = None,
        report_v2_id: Optional[int] = None,
    ) -> None:
        """Create a failed delivery_queue record for later retry."""
        async with async_session() as db:
            item = DeliveryQueue(
                session_id=session_id,
                report_v1_id=report_v1_id,
                report_v2_id=report_v2_id,
                status="failed",
                retry_count=1,
            )
            db.add(item)
            await db.commit()


# Module-level singleton
publisher = RealtimePublisher()
