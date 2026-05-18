"""
Cloud Run Job entry-point for the BimariHaunter scraper.

Usage (local):
    python scraper_job.py

Usage (with specific sources):
    SOURCE_IDS=1,3,5 python scraper_job.py

In production this is executed by a Cloud Run Job triggered via
Cloud Scheduler or the /api/v1/jobs/trigger endpoint.
"""

from __future__ import annotations

import asyncio
import os
import sys

import structlog

logger = structlog.get_logger(__name__)


async def main() -> None:
    from app.config import settings
    from app.scraper.scheduler import ScrapeScheduler

    # Parse optional SOURCE_IDS filter
    source_ids_raw = os.getenv("SOURCE_IDS", "")
    source_ids = None
    if source_ids_raw.strip():
        try:
            source_ids = [int(s.strip()) for s in source_ids_raw.split(",")]
            logger.info("filtering_sources", ids=source_ids)
        except ValueError:
            logger.error("invalid_source_ids", raw=source_ids_raw)
            sys.exit(1)

    scheduler = ScrapeScheduler(
        max_concurrent=settings.max_concurrent_scrapers,
    )

    logger.info("scraper_job_started")
    await scheduler.run_all(source_ids=source_ids)
    logger.info("scraper_job_completed")


if __name__ == "__main__":
    asyncio.run(main())
