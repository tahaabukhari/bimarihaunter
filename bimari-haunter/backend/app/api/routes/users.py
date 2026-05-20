from fastapi import APIRouter, Depends, HTTPException
from typing import Dict, Any
from google.cloud import firestore
from datetime import datetime, timezone

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


@router.get("/search")
async def search_users(
    query: str,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Search for users by name (prefix match), email (exact match), or UID (exact match).
    Filters out the current requesting user from results.
    """
    try:
        current_uid = user_token["uid"]
        search_query = query.strip()
        if not search_query:
            return {"users": []}

        users_ref = db.collection("users")
        results = {}

        # 1. Search by email (exact)
        email_docs = users_ref.where("email", "==", search_query).stream()
        for doc in email_docs:
            data = doc.to_dict()
            if data.get("uid") != current_uid:
                results[data["uid"]] = {
                    "uid": data.get("uid"),
                    "name": data.get("name", "Unknown"),
                    "email": data.get("email", ""),
                    "initials": "".join([part[0] for part in data.get("name", "Unknown").split() if part]).upper()[:2]
                }

        # 2. Search by UID (exact)
        uid_doc = users_ref.document(search_query).get()
        if uid_doc.exists:
            data = uid_doc.to_dict()
            if data.get("uid") != current_uid:
                results[data["uid"]] = {
                    "uid": data.get("uid"),
                    "name": data.get("name", "Unknown"),
                    "email": data.get("email", ""),
                    "initials": "".join([part[0] for part in data.get("name", "Unknown").split() if part]).upper()[:2]
                }

        # 3. Search by name (prefix match)
        # Search for query as-is, and with capitalization
        for prefix in [search_query, search_query.capitalize()]:
            name_docs = users_ref.where("name", ">=", prefix).where("name", "<=", prefix + "\uf8ff").limit(20).stream()
            for doc in name_docs:
                data = doc.to_dict()
                if data.get("uid") != current_uid:
                    results[data["uid"]] = {
                        "uid": data.get("uid"),
                        "name": data.get("name", "Unknown"),
                        "email": data.get("email", ""),
                        "initials": "".join([part[0] for part in data.get("name", "Unknown").split() if part]).upper()[:2]
                    }

        return {"users": list(results.values())}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to search users: {str(e)}")


@router.post("/friends/{friend_id}")
async def add_friend(
    friend_id: str,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Adds a mutual friend connection between the current user and friend_id.
    """
    try:
        user_id = user_token["uid"]
        if user_id == friend_id:
            raise HTTPException(status_code=400, detail="You cannot add yourself as a friend.")

        # Check if friend exists in users collection
        friend_doc = db.collection("users").document(friend_id).get()
        if not friend_doc.exists:
            raise HTTPException(status_code=404, detail="User not found.")
        friend_data = friend_doc.to_dict()

        # Check if current user document exists to get their name/email
        user_doc = db.collection("users").document(user_id).get()
        if not user_doc.exists:
            # Fallback to token details if user document isn't initialized yet
            user_name = user_token.get("name", "Unknown")
            user_email = user_token.get("email", "")
        else:
            user_data = user_doc.to_dict()
            user_name = user_data.get("name", user_token.get("name", "Unknown"))
            user_email = user_data.get("email", user_token.get("email", ""))

        # 1. Add B to A's friends subcollection
        db.collection("users").document(user_id).collection("friends").document(friend_id).set({
            "uid": friend_id,
            "name": friend_data.get("name", "Unknown"),
            "email": friend_data.get("email", ""),
            "added_at": firestore.SERVER_TIMESTAMP
        })

        # 2. Add A to B's friends subcollection
        db.collection("users").document(friend_id).collection("friends").document(user_id).set({
            "uid": user_id,
            "name": user_name,
            "email": user_email,
            "added_at": firestore.SERVER_TIMESTAMP
        })

        return {"status": "success", "message": f"Successfully added {friend_data.get('name')} as friend."}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to add friend: {str(e)}")


@router.delete("/friends/{friend_id}")
async def remove_friend(
    friend_id: str,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Removes the mutual friend connection between current user and friend_id.
    """
    try:
        user_id = user_token["uid"]
        
        # Remove mutual links
        db.collection("users").document(user_id).collection("friends").document(friend_id).delete()
        db.collection("users").document(friend_id).collection("friends").document(user_id).delete()

        return {"status": "success", "message": "Friend removed successfully."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to remove friend: {str(e)}")


@router.get("/friends")
async def list_friends(
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Lists all friends of the authenticated user.
    """
    try:
        user_id = user_token["uid"]
        friends_ref = db.collection("users").document(user_id).collection("friends").order_by("added_at", direction="DESCENDING")
        docs = friends_ref.stream()

        friends = []
        for doc in docs:
            data = doc.to_dict()
            if isinstance(data.get("added_at"), datetime):
                data["added_at"] = data["added_at"].isoformat()
            friends.append(data)

        return {"friends": friends}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch friends: {str(e)}")


@router.post("/block/{blocked_id}")
async def block_user(
    blocked_id: str,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Blocks a user. Automatically removes them from friends.
    """
    try:
        user_id = user_token["uid"]
        if user_id == blocked_id:
            raise HTTPException(status_code=400, detail="You cannot block yourself.")

        # Check if user exists
        blocked_doc = db.collection("users").document(blocked_id).get()
        if not blocked_doc.exists:
            raise HTTPException(status_code=404, detail="User to block not found.")

        # 1. Add to block collection
        db.collection("users").document(user_id).collection("blocked").document(blocked_id).set({
            "uid": blocked_id,
            "blocked_at": firestore.SERVER_TIMESTAMP
        })

        # 2. Automatically remove as friends if they were
        db.collection("users").document(user_id).collection("friends").document(blocked_id).delete()
        db.collection("users").document(blocked_id).collection("friends").document(user_id).delete()

        return {"status": "success", "message": "User blocked successfully."}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to block user: {str(e)}")


@router.delete("/block/{blocked_id}")
async def unblock_user(
    blocked_id: str,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Unblocks a user.
    """
    try:
        user_id = user_token["uid"]
        db.collection("users").document(user_id).collection("blocked").document(blocked_id).delete()
        return {"status": "success", "message": "User unblocked successfully."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to unblock user: {str(e)}")


@router.get("/blocked")
async def list_blocked_users(
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Lists UIDs of all blocked users.
    """
    try:
        user_id = user_token["uid"]
        blocked_ref = db.collection("users").document(user_id).collection("blocked").stream()
        blocked_uids = [doc.id for doc in blocked_ref]
        return {"blocked": blocked_uids}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch blocked list: {str(e)}")


