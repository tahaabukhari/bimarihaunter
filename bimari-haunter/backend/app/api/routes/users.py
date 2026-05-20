from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from typing import Dict, Any
from google.cloud import firestore

from app.services.firebase_auth import verify_firebase_token
from app.database.firestore import db
from app.api.schemas import LocationUpdateRequest, UserRegisterRequest, UserPreferencesRequest
from app.api.routes.jobs import run_scraper_background


router = APIRouter(prefix="/users", tags=["users"])

@router.get("/me")
async def get_current_user(user_token: dict = Depends(verify_firebase_token)) -> Dict[str, Any]:
    """
    Returns the authenticated user's Firebase token payload.
    This acts as a verification endpoint for the frontend.
    """
    return {
        "status": "authenticated",
        "uid": user_token.get("uid"),
        "email": user_token.get("email"),
        "name": user_token.get("name", "Unknown"),
    }

@router.post("/location")
async def update_user_location(
    location: LocationUpdateRequest,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Updates the user's live coordinates and city.
    Immediately synchronizes and populates their personalized /users/{userId}/feed
    with the 50 most recent localized reports.
    """
    try:
        user_id = user_token["uid"]
        user_ref = db.collection("users").document(user_id)
        
        # 1. Update user profile
        user_ref.set({
            "uid": user_id,
            "email": user_token.get("email"),
            "name": user_token.get("name", "Unknown"),
            "live_location": {
                "city": location.city,
                "coordinates": firestore.GeoPoint(location.latitude, location.longitude)
            },
            "updated_at": firestore.SERVER_TIMESTAMP
        }, merge=True)
        
        # 2. Fetch up to 50 most recent matching reports from global collection
        city_capitalized = location.city.strip().capitalize()
        reports_ref = db.collection("reports")
        
        # Search reports where city is in the resolved location array
        query = (
            reports_ref
            .where("ai_analysis.locations", "array_contains", city_capitalized)
            .limit(100)
        )
        matched_docs = list(query.stream())
        
        # Sort in memory to avoid composite index requirements
        def get_pub_date(d):
            dt = d.to_dict().get("published_at")
            if isinstance(dt, datetime):
                return dt
            return datetime.min.replace(tzinfo=timezone.utc)
            
        matched_docs.sort(key=get_pub_date, reverse=True)
        matched_docs = matched_docs[:50]
        
        feed_ref = user_ref.collection("feed")
        batch = db.batch()
        count = 0
        
        for doc in matched_docs:
            doc_data = doc.to_dict()
            feed_doc_ref = feed_ref.document(doc.id)
            batch.set(feed_doc_ref, doc_data)
            count += 1
            
        if count > 0:
            batch.commit()
            
        # 3. Apply Capped Stack (FIFO) limit of 50 documents
        current_feed_docs = feed_ref.order_by("published_at", direction="DESCENDING").stream()
        for idx, f_doc in enumerate(current_feed_docs):
            if idx >= 50:
                feed_ref.document(f_doc.id).delete()
                
        return {
            "status": "success",
            "message": f"Successfully updated location to {location.city} and synchronized {count} local feed posts.",
            "city": location.city,
            "feed_count": min(count, 50)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to update location/feed: {str(e)}")


@router.post("/register")
async def register_user(
    user_data: UserRegisterRequest
) -> Dict[str, Any]:
    """
    Registers a new user and writes their details to Firestore.
    """
    try:
        user_ref = db.collection("users").document(user_data.uid)
        user_ref.set({
            "uid": user_data.uid,
            "email": user_data.email,
            "name": user_data.name,
            "phone_number": user_data.phone_number,
            "avatar_url": user_data.avatar_url,
            "initials": "".join([part[0].upper() for part in user_data.name.split() if part])[:2],
            "created_at": firestore.SERVER_TIMESTAMP,
            "updated_at": firestore.SERVER_TIMESTAMP
        }, merge=True)
        return {
            "status": "success",
            "message": "User registered successfully",
            "uid": user_data.uid
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to register user: {str(e)}")


@router.post("/{uid}/preferences")
async def update_user_preferences(
    uid: str,
    prefs: UserPreferencesRequest,
    background_tasks: BackgroundTasks
) -> Dict[str, Any]:
    """
    Updates the user's outbreak preferences (diseases, location, radius).
    Immediately triggers a localized scraper job in the background to refresh their feed.
    """
    try:
        user_ref = db.collection("users").document(uid)
        
        # 1. Update preferences and location in user profile
        user_ref.set({
            "preferences": {
                "diseases": prefs.diseases,
                "radius": prefs.radius,
                "city": prefs.city,
                "coordinates": firestore.GeoPoint(prefs.latitude, prefs.longitude)
            },
            # Also sync live_location for backwards compatibility
            "live_location": {
                "city": prefs.city,
                "coordinates": firestore.GeoPoint(prefs.latitude, prefs.longitude)
            },
            "updated_at": firestore.SERVER_TIMESTAMP
        }, merge=True)
        
        # 2. Trigger the instant scraper in the background
        background_tasks.add_task(
            run_scraper_background,
            user_id=uid,
            city=prefs.city,
            lat=prefs.latitude,
            lon=prefs.longitude
        )
        
        return {
            "status": "success",
            "message": "Preferences updated and background scrape job triggered successfully."
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to update preferences: {str(e)}")
