"""
Feed endpoint — serves personalized and global outbreak reports from Firestore.

Query strategy (in order of preference):
1. User's personalized sub-collection: users/{uid}/feed
2. Global /reports filtered by status == "analyzed"
3. Global /reports with no filter (catches any data regardless of status)

All Firestore GeoPoints are serialized to {latitude, longitude} dicts for
Android Kotlin JSON parsing compatibility.
"""
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List, Dict, Any
from app.services.firebase_auth import verify_firebase_token
from app.database.firestore import db

import structlog
logger = structlog.get_logger(__name__)

router = APIRouter(prefix="/feed", tags=["feed"])


def _serialize_doc(doc_id: str, data: dict) -> dict:
    """Normalize a Firestore document for JSON serialization."""
    data["id"] = doc_id

    # Serialize GeoPoints inside ai_analysis
    ai = data.get("ai_analysis")
    if isinstance(ai, dict):
        coords = ai.get("coordinates")
        if coords is not None and hasattr(coords, "latitude"):
            ai["coordinates"] = {
                "latitude": coords.latitude,
                "longitude": coords.longitude,
            }

    # Serialize all datetime fields at top level
    for key, val in list(data.items()):
        if isinstance(val, datetime):
            data[key] = val.isoformat()

    # Serialize datetime fields inside ai_analysis
    if isinstance(ai, dict):
        for key, val in list(ai.items()):
            if isinstance(val, datetime):
                ai[key] = val.isoformat()

    return data


@router.get("")
async def get_feed(
    limit: int = Query(50, ge=1, le=100),
    user_token: dict = Depends(verify_firebase_token),
) -> List[Dict[str, Any]]:
    """
    Returns personalized outbreak feed for the authenticated user.

    Falls back to global reports if the user's personalized feed is empty.
    Handles missing Firestore indexes gracefully by trying simpler queries.
    """
    user_id = user_token.get("uid", "")

    # ── 1. Try personalized feed ───────────────────────────
    try:
        personal_docs = list(
            db.collection("users")
            .document(user_id)
            .collection("feed")
            .order_by("published_at", direction="DESCENDING")
            .limit(limit)
            .stream()
        )
        if personal_docs:
            logger.info("serving_personalized_feed", user_id=user_id, count=len(personal_docs))
            return [_serialize_doc(d.id, d.to_dict()) for d in personal_docs]
    except Exception as e:
        logger.warning("personalized_feed_query_failed", user_id=user_id, error=str(e))

    # ── 2. Try global reports filtered by status="analyzed" ──
    try:
        analyzed_docs = list(
            db.collection("reports")
            .where("status", "==", "analyzed")
            .limit(limit)
            .stream()
        )
        if analyzed_docs:
            logger.info("serving_global_analyzed_feed", count=len(analyzed_docs))
            return [_serialize_doc(d.id, d.to_dict()) for d in analyzed_docs]
    except Exception as e:
        logger.warning("global_analyzed_feed_query_failed", error=str(e))

    # ── 3. Last resort: any reports in collection ──────────
    try:
        all_docs = list(
            db.collection("reports")
            .limit(limit)
            .stream()
        )
        if all_docs:
            logger.info("serving_all_reports_fallback", count=len(all_docs))
            return [_serialize_doc(d.id, d.to_dict()) for d in all_docs]
    except Exception as e:
        logger.error("all_reports_query_failed", error=str(e))
        raise HTTPException(status_code=500, detail=f"Feed unavailable: {str(e)}")

    # ── 4. Empty but healthy response ─────────────────────
    logger.info("feed_empty_no_reports_yet", user_id=user_id)
    return []
