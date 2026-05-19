from fastapi import APIRouter, Depends, HTTPException
from typing import Dict, Any
from google.cloud import firestore

from app.services.firebase_auth import verify_firebase_token
from app.database.firestore import db
from app.api.schemas import UserLocationUpdate

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
    location: UserLocationUpdate,
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
            .order_by("published_at", direction="DESCENDING")
            .limit(50)
        )
        matched_docs = query.stream()
        
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

