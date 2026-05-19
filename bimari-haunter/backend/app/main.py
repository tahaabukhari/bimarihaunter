"""
FastAPI application entry-point.

Assembles all routers, starts background tasks (delivery queue
processor) on startup, and cancels them on shutdown.
"""

from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from typing import AsyncGenerator

import structlog
from fastapi import FastAPI

from app.api.routes import jobs, reports, sources, websocket
from app.publisher.queue_processor import run_delivery_queue

logger = structlog.get_logger(__name__)

# Background task references
_background_tasks: list[asyncio.Task] = []


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Startup / shutdown lifecycle manager."""
    logger.info("starting_background_tasks")

    # Start delivery-queue processor
    task = asyncio.create_task(run_delivery_queue())
    _background_tasks.append(task)

    yield

    # Cancel background tasks
    logger.info("shutting_down_background_tasks")
    for task in _background_tasks:
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass
    _background_tasks.clear()
    logger.info("shutdown_complete")


app = FastAPI(
    title="BimariHaunter",
    description=(
        "Health / disease outbreak monitoring backend for Pakistan. "
        "Scrapes news & social media, processes with NLP, and pushes "
        "real-time reports to Android clients via WebSocket."
    ),
    version="0.1.0",
    lifespan=lifespan,
)

# ── Include routers ─────────────────────────────────────────

from app.api.routes import jobs, reports, sources, websocket, users, feed, chats, groups

# ...

app.include_router(sources.router)
app.include_router(jobs.router)
app.include_router(reports.router)
app.include_router(websocket.router)
app.include_router(users.router, prefix="/api/v1")
app.include_router(feed.router, prefix="/api/v1")
app.include_router(chats.router, prefix="/api/v1")
app.include_router(groups.router, prefix="/api/v1")



# ── Health check ────────────────────────────────────────────


@app.get("/health")
async def health_check():
    return {
        "status": "ok",
        "service": "bimari-haunter",
    }
