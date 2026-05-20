"""
API routes for Community Outbreak discussion groups.

Supports regional group discovery, group creation, message posting,
and real-time message log retrieval for local community outbreak updates.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional
from fastapi import APIRouter, Depends, HTTPException
from google.cloud import firestore
from pydantic import BaseModel

from app.database.firestore import db
from app.services.firebase_auth import verify_firebase_token

router = APIRouter(prefix="/groups", tags=["groups"])

# ── Schemas ──────────────────────────────────────────────────

class GroupCreate(BaseModel):
    name: str
    city: str

class GroupMessageCreate(BaseModel):
    text: str

# ── API Routes ───────────────────────────────────────────────

@router.get("/")
async def list_groups(
    city: Optional[str] = None,
    user_token: dict = Depends(verify_firebase_token)
) -> List[Dict[str, Any]]:
    """Lists community outbreak discussion groups. Can filter by city."""
    try:
        groups_ref = db.collection("groups")
        if city:
            city_cap = city.strip().capitalize()
            query = groups_ref.where("city", "==", city_cap).order_by("created_at", direction="DESCENDING")
        else:
            query = groups_ref.order_by("created_at", direction="DESCENDING")
            
        docs = query.stream()
        
        groups = []
        for doc in docs:
            data = doc.to_dict()
            data["group_id"] = doc.id
            if isinstance(data.get("created_at"), datetime):
                data["created_at"] = data["created_at"].isoformat()
            groups.append(data)
        return groups
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list groups: {str(e)}")

@router.post("/")
async def create_group(
    group_input: GroupCreate,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """Creates a new regional community outbreak group."""
    try:
        user_id = user_token["uid"]
        city_cap = group_input.city.strip().capitalize()
        
        group_ref = db.collection("groups").document()
        group_id = group_ref.id
        
        group_ref.set({
            "group_id": group_id,
            "name": group_input.name,
            "city": city_cap,
            "created_by": user_id,
            "members": [user_id],
            "created_at": firestore.SERVER_TIMESTAMP
        })
        
        return {
            "status": "success",
            "group_id": group_id,
            "name": group_input.name,
            "city": city_cap
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to create group: {str(e)}")

@router.get("/{group_id}/messages")
async def get_group_messages(
    group_id: str,
    limit: int = 100,
    user_token: dict = Depends(verify_firebase_token)
) -> List[Dict[str, Any]]:
    """Retrieves messages for a regional community group."""
    try:
        # Check group existence
        group_doc = db.collection("groups").document(group_id).get()
        if not group_doc.exists:
            raise HTTPException(status_code=404, detail="Community group not found.")
            
        messages_ref = db.collection("groups").document(group_id).collection("messages")
        query = messages_ref.order_by("timestamp", direction="ASCENDING").limit(limit)
        docs = query.stream()
        
        messages = []
        for doc in docs:
            data = doc.to_dict()
            data["message_id"] = doc.id
            if isinstance(data.get("timestamp"), datetime):
                data["timestamp"] = data["timestamp"].isoformat()
            messages.append(data)
        return messages
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch group messages: {str(e)}")

@router.post("/{group_id}/messages")
async def send_group_message(
    group_id: str,
    msg_input: GroupMessageCreate,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """Posts a message/alert to the community group board."""
    try:
        user_id = user_token["uid"]
        
        # Verify group exists
        group_ref = db.collection("groups").document(group_id)
        group_doc = group_ref.get()
        if not group_doc.exists:
            raise HTTPException(status_code=404, detail="Community group not found.")
            
        messages_ref = group_ref.collection("messages")
        msg_ref = messages_ref.document()
        
        # Post the message
        msg_ref.set({
            "sender_id": user_id,
            "sender_name": user_token.get("name", "Community Member"),
            "text": msg_input.text,
            "timestamp": firestore.SERVER_TIMESTAMP
        })
        
        # Join user to group members list dynamically if they aren't listed
        members = group_doc.to_dict().get("members", [])
        if user_id not in members:
            group_ref.update({
                "members": firestore.ArrayUnion([user_id])
            })
            
        return {
            "status": "success",
            "message_id": msg_ref.id,
            "text": msg_input.text
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send group message: {str(e)}")
