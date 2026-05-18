"""
WebSocket endpoint at /ws/{device_id}.

Handles:
  • subscribe       – update user_preferences, ack with active_insights count
  • request_full_report – queue V2 delivery
  • mark_read       – mark a report as read
  • heartbeat       – pong
"""

from __future__ import annotations

from datetime import datetime, timezone

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.engine import async_session
from app.database.models import AppSession, ReportV1Insight, ReportV2Full
from app.publisher.realtime import publisher

router = APIRouter()


@router.websocket("/ws/{device_id}")
async def websocket_endpoint(websocket: WebSocket, device_id: str):
    """Main WebSocket connection handler for Android clients."""
    await websocket.accept()

    # Register connection
    publisher.register(device_id, websocket)

    # Register or update app session
    session_id: int | None = None
    async with async_session() as db:
        stmt = select(AppSession).where(AppSession.device_id == device_id)
        result = await db.execute(stmt)
        session = result.scalar_one_or_none()

        if session is None:
            session = AppSession(
                device_id=device_id,
                last_active_at=datetime.now(timezone.utc),
            )
            db.add(session)
            await db.flush()
            await db.refresh(session)
        else:
            session.last_active_at = datetime.now(timezone.utc)

        session_id = session.id
        await db.commit()

    try:
        while True:
            data = await websocket.receive_json()
            action = data.get("action")

            if action == "subscribe":
                await _handle_subscribe(websocket, device_id, data, session_id)

            elif action == "request_full_report":
                await _handle_request_full_report(
                    websocket, data, session_id
                )

            elif action == "mark_read":
                await _handle_mark_read(websocket, data)

            elif action == "heartbeat":
                await websocket.send_json({"action": "pong"})

            else:
                await websocket.send_json(
                    {"error": f"unknown action: {action}"}
                )

    except WebSocketDisconnect:
        publisher.unregister(device_id)
    except Exception:
        publisher.unregister(device_id)


# ── Action handlers ─────────────────────────────────────────


async def _handle_subscribe(
    ws: WebSocket,
    device_id: str,
    data: dict,
    session_id: int | None,
) -> None:
    """Update user_preferences and respond with active insight count."""
    preferences = data.get("preferences", {})

    async with async_session() as db:
        if session_id:
            session = await db.get(AppSession, session_id)
            if session:
                session.user_preferences = preferences
                session.last_active_at = datetime.now(timezone.utc)
                await db.commit()

        active_count = await db.scalar(
            select(func.count())
            .select_from(ReportV1Insight)
            .where(ReportV1Insight.delivery_status != "expired")
        )

    await ws.send_json(
        {
            "action": "ack",
            "subscription": "updated",
            "active_insights": active_count or 0,
        }
    )


async def _handle_request_full_report(
    ws: WebSocket,
    data: dict,
    session_id: int | None,
) -> None:
    """Queue a V2 full report for delivery."""
    report_id = data.get("report_id")
    if not report_id:
        await ws.send_json({"error": "report_id required"})
        return

    await publisher.publish_full_report(
        report_id, request_session_id=session_id
    )

    await ws.send_json(
        {
            "action": "full_report_queued",
            "report_id": report_id,
        }
    )


async def _handle_mark_read(ws: WebSocket, data: dict) -> None:
    """Mark a V1 or V2 report as read."""
    report_id = data.get("report_id")
    version = data.get("version", "v1")

    if not report_id:
        await ws.send_json({"error": "report_id required"})
        return

    async with async_session() as db:
        if version == "v1":
            report = await db.get(ReportV1Insight, report_id)
        else:
            report = await db.get(ReportV2Full, report_id)

        if report:
            report.read_at = datetime.now(timezone.utc)
            await db.commit()

    await ws.send_json(
        {
            "action": "marked_read",
            "report_id": report_id,
            "version": version,
        }
    )
