"""
FastAPI application entry-point.
All data is stored in and served from Firestore — no PostgreSQL required.
"""

from __future__ import annotations

import structlog
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

logger = structlog.get_logger(__name__)


app = FastAPI(
    title="BimariHaunter",
    description=(
        "Health / disease outbreak monitoring backend for Pakistan. "
        "Scrapes news, processes with keyword NLP, and serves "
        "real-time reports to Android clients via Firestore."
    ),
    version="0.2.0",
)

# ── CORS ─────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Include routers ─────────────────────────────────────────
from app.api.routes import jobs, users, feed, chats, groups, maps

app.include_router(jobs.router,   prefix="/api/v1")
app.include_router(users.router,  prefix="/api/v1")
app.include_router(feed.router,   prefix="/api/v1")
app.include_router(chats.router,  prefix="/api/v1")
app.include_router(groups.router, prefix="/api/v1")
app.include_router(maps.router,   prefix="/api/v1")



# ── Health check ────────────────────────────────────────────


@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "bimari-haunter", "version": "0.2.0"}

@app.get("/")
async def root():
    return {"message": "BimariHaunter API is running", "docs": "/docs"}


# ── Startup Daemon (Scraper execution every 4 hours) ────────
import asyncio

async def scraper_cron_loop():
    logger.info("Starting scraper_cron_loop background daemon (interval: 4 hours)")
    # Initial sleep of 10 seconds to allow the backend to bind and complete start-up checks
    await asyncio.sleep(10)
    while True:
        try:
            logger.info("Triggering background scheduled scrape...")
            from app.api.routes.jobs import run_scraper_background
            await run_scraper_background()
            logger.info("Scheduled scrape iteration complete.")
        except Exception as e:
            logger.error("Error in scheduled scraper execution", error=str(e))
        # Wait 4 hours (14400 seconds)
        await asyncio.sleep(14400)

@app.on_event("startup")
async def startup_event():
    logger.info("FastAPI backend initialized, starting background scraper scheduler.")
    asyncio.create_task(scraper_cron_loop())
