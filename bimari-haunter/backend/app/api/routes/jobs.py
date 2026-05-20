"""
Scrape-job management endpoints.
"""

from __future__ import annotations

from datetime import datetime, timezone
from fastapi import APIRouter, BackgroundTasks, HTTPException, Depends
from typing import List, Dict, Any, Optional

from app.database.firestore import db
from app.scraper.scheduler import ScrapeScheduler
from app.config import settings
from app.services.firebase_auth import verify_firebase_token

router = APIRouter(prefix="/api/v1", tags=["jobs"])


async def run_scraper_background(
    user_id: Optional[str] = None,
    city: Optional[str] = None,
    lat: Optional[float] = None,
    lon: Optional[float] = None,
):
    """Executes the scraper scheduler asynchronously."""
    try:
        scheduler = ScrapeScheduler(
            max_concurrent=settings.max_concurrent_scrapers,
        )
        await scheduler.run_all(user_id=user_id, city=city, lat=lat, lon=lon)
    except Exception as e:
        import structlog
        logger = structlog.get_logger(__name__)
        logger.error("background_scraper_failed", error=str(e))


@router.get("/jobs")
async def list_jobs(limit: int = 20) -> List[Dict[str, Any]]:
    """List the most recent scrape jobs from Firestore."""
    try:
        jobs_ref = db.collection("scrape_jobs")
        docs = (
            jobs_ref
            .order_by("started_at", direction="DESCENDING")
            .limit(limit)
            .stream()
        )
        
        jobs = []
        for doc in docs:
            data = doc.to_dict()
            data["id"] = doc.id
            
            # Serialize datetimes to ISO format for JSON compatibility
            for key, val in data.items():
                if isinstance(val, datetime):
                    data[key] = val.isoformat()
                    
            jobs.append(data)
        return jobs
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list jobs: {str(e)}")


@router.post("/jobs/trigger", status_code=202)
async def trigger_job(
    background_tasks: BackgroundTasks,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """Manual trigger endpoint.
    
    Executes the scraper locally in a non-blocking background task.
    """
    user_id = user_token.get("uid")
    city = None
    lat = None
    lon = None
    
    if user_id:
        try:
            user_doc = db.collection("users").document(user_id).get()
            if user_doc.exists:
                user_data = user_doc.to_dict()
                live_loc = user_data.get("live_location", {})
                if live_loc:
                    city = live_loc.get("city")
                    coords = live_loc.get("coordinates")
                    if coords:
                        lat = coords.latitude
                        lon = coords.longitude
        except Exception as e:
            import structlog
            logger = structlog.get_logger(__name__)
            logger.error("failed_to_fetch_user_location_for_trigger", user_id=user_id, error=str(e))

    background_tasks.add_task(
        run_scraper_background,
        user_id=user_id,
        city=city,
        lat=lat,
        lon=lon
    )
    return {
        "status": "accepted",
        "message": "Scraper job triggered in the background.",
    }
