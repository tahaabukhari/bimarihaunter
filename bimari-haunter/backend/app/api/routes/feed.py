from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from typing import List, Dict, Any
from app.services.firebase_auth import verify_firebase_token
from app.database.firestore import db
from google.cloud import firestore

router = APIRouter(prefix="/feed", tags=["feed"])

@router.get("/")
async def get_feed(
    limit: int = 50,
    user_token: dict = Depends(verify_firebase_token)
) -> List[Dict[str, Any]]:
    """
    Retrieves the personalized location-aware health reports for the authenticated user.
    Falls back dynamically to global outbreak reports if the personalized feed is empty or has no location set.
    """
    try:
        user_id = user_token["uid"]
        
        # 1. Attempt to query personalized feed first
        personal_feed_ref = (
            db.collection("users")
            .document(user_id)
            .collection("feed")
            .order_by("published_at", direction="DESCENDING")
            .limit(limit)
        )
        docs = list(personal_feed_ref.stream())
        
        # 2. Fallback to global reports if personalized feed is empty
        if not docs:
            global_reports_ref = (
                db.collection("reports")
                .order_by("published_at", direction="DESCENDING")
                .limit(limit)
            )
            docs = global_reports_ref.stream()
        
        feed_data = []
        for doc in docs:
            data = doc.to_dict()
            data['id'] = doc.id
            
            # Custom serialization for GeoPoints (for regional maps in Kotlin)
            if 'ai_analysis' in data and isinstance(data['ai_analysis'], dict):
                coords = data['ai_analysis'].get('coordinates')
                if coords and hasattr(coords, 'latitude') and hasattr(coords, 'longitude'):
                    data['ai_analysis']['coordinates'] = {
                        "latitude": coords.latitude,
                        "longitude": coords.longitude
                    }
                    
            # Serialize datetime objects to ISO strings for JSON compatibility
            for key, val in data.items():
                if isinstance(val, datetime):
                    data[key] = val.isoformat()
            if 'ai_analysis' in data and isinstance(data['ai_analysis'], dict):
                for key, val in data['ai_analysis'].items():
                    if isinstance(val, datetime):
                        data['ai_analysis'][key] = val.isoformat()
            
            feed_data.append(data)
            
        return feed_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch feed: {str(e)}")


