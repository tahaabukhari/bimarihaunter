from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List, Dict, Any, Optional
from google.cloud import firestore

from app.services.firebase_auth import verify_firebase_token
from app.database.firestore import db

router = APIRouter(prefix="/maps", tags=["maps"])

@router.get("/markers")
async def get_map_markers(
    disease: Optional[str] = Query(None, description="Filter markers by disease key"),
    severity: Optional[str] = Query(None, description="Filter markers by severity: high, medium, low"),
    limit: int = Query(100, le=300, description="Max number of markers to return"),
    user_token: dict = Depends(verify_firebase_token)
) -> List[Dict[str, Any]]:
    """
    Retrieves geolocated outbreak markers from Firestore to render on the global Android Datamap.
    Filters out any reports that do not have valid geolocation coordinates.
    """
    try:
        reports_ref = db.collection("reports")
        
        # 1. Standard base query ordered by recency
        query = reports_ref.order_by("published_at", direction="DESCENDING")
        
        # Limit the initial stream to avoid high overhead
        docs = query.limit(limit * 4).stream()
        
        markers = []
        for doc in docs:
            data = doc.to_dict()
            analysis = data.get("ai_analysis")
            
            if not analysis or not isinstance(analysis, dict):
                continue
                
            # Filter by disease server-side if query parameter provided
            # (Firestore doesn't allow inequality/ordering mixed with array_contains easily without complex indexes)
            rep_disease = analysis.get("disease", "")
            if disease and disease.lower() != rep_disease.lower():
                continue
                
            # Filter by severity in-memory to prevent complex index requirement
            rep_severity = analysis.get("severity", "medium")
            if severity and severity.lower() != rep_severity.lower():
                continue
                
            coords = analysis.get("coordinates")
            if not coords or not hasattr(coords, "latitude") or not hasattr(coords, "longitude"):
                continue
                
            # Convert published_at to ISO string format
            pub_at = data.get("published_at")
            pub_at_str = ""
            if isinstance(pub_at, datetime):
                pub_at_str = pub_at.isoformat()
            elif pub_at:
                pub_at_str = str(pub_at)
                
            markers.append({
                "id": doc.id,
                "title": data.get("title", "Outbreak Report"),
                "disease": rep_disease,
                "severity": analysis.get("severity", "medium"),
                "latitude": coords.latitude,
                "longitude": coords.longitude,
                "summary": analysis.get("summary", []),
                "symptoms": analysis.get("symptoms", []),
                "source": data.get("source", "Unknown Source"),
                "published_at": pub_at_str
            })
            
            if len(markers) >= limit:
                break
                
        return markers
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch map markers: {str(e)}")
