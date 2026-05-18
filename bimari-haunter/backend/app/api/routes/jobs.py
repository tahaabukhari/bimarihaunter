"""
Scrape-job management endpoints.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.schemas import JobResponse
from app.database.engine import get_db
from app.database.models import ScrapeJob

router = APIRouter(prefix="/api/v1", tags=["jobs"])


@router.get("/jobs", response_model=list[JobResponse])
async def list_jobs(
    limit: int = 20,
    db: AsyncSession = Depends(get_db),
):
    """List the most recent scrape jobs."""
    stmt = (
        select(ScrapeJob)
        .order_by(ScrapeJob.created_at.desc())
        .limit(limit)
    )
    result = await db.execute(stmt)
    return result.scalars().all()


@router.post("/jobs/trigger")
async def trigger_job():
    """Manual trigger endpoint.

    In production this connects to the Cloud Run Jobs API to
    launch a scraper job execution.  For local development the
    ``scraper_job.py`` entry-point can be run directly.
    """
    return {
        "message": (
            "In production, this endpoint triggers a Cloud Run Job. "
            "For local testing, run `python scraper_job.py` directly."
        ),
        "status": "acknowledged",
    }
