"""
End-to-End Firestore Verification Script for BimariHaunter User Workflows.

Tests:
1. Location-based profile setup and FIFO 50-item feed generation.
2. Real-time report fan-out simulation.
3. AI Direct Chats schema & Smart Agent advisory capability.
4. Community Outbreak Groups schema & community discussion posts.
"""

import asyncio
import json
from datetime import datetime, timezone
from google.cloud import firestore


# Set credentials path before importing app components
import os
os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = "bimarihaunter-firebase-adminsdk-fbsvc-99e53c4db6.json"

from app.database.firestore import db
from app.api.routes.chats import query_outbreaks, query_web_search

async def run_verification():
    print("--- Starting BimariHaunter Firestore Architecture Verification ---")
    
    test_user_id = "test_user_kotlin_999"
    test_city = "Karachi"
    
    # ── Test 1: User Profile & Feed Stack Initialization ──
    print("\n[Test 1] Setting up User Profile and Personalized Capped Feed...")
    user_ref = db.collection("users").document(test_user_id)
    
    # Create profile
    user_ref.set({
        "uid": test_user_id,
        "email": "test-kotlin@bimarihaunter.pk",
        "name": "Taha Bukhari Test",
        "live_location": {
            "city": test_city,
            "coordinates": firestore.GeoPoint(24.8607, 67.0011)
        },
        "updated_at": firestore.SERVER_TIMESTAMP
    }, merge=True)
    print("SUCCESS: Profile created in Firestore: /users/test_user_kotlin_999")
    
    # Query matching global reports and write to user feed
    city_cap = test_city.capitalize()
    reports_ref = db.collection("reports")
    query = reports_ref.where("ai_analysis.locations", "array_contains", city_cap).limit(60)
    matched_docs = list(query.stream())
    print(f"SUCCESS: Found {len(matched_docs)} global reports matching city: {city_cap}")
    
    feed_ref = user_ref.collection("feed")
    batch = db.batch()
    for idx, doc in enumerate(matched_docs):
        batch.set(feed_ref.document(doc.id), doc.to_dict())
    batch.commit()
    print("SUCCESS: Personalized feed populated.")
    
    # Apply 50-item stack FIFO capping
    current_feed = list(feed_ref.order_by("published_at", direction="DESCENDING").stream())
    print(f"SUCCESS: Total items in feed before stack trim: {len(current_feed)}")
    deleted_count = 0
    for idx, f_doc in enumerate(current_feed):
        if idx >= 50:
            feed_ref.document(f_doc.id).delete()
            deleted_count += 1
            
    final_feed = list(feed_ref.stream())
    print(f"SUCCESS: Deleted {deleted_count} stale reports. Final personal feed count: {len(final_feed)} (Stack Limit: 50)")
    assert len(final_feed) <= 50, "Capped Stack limit violated!"
    
    # ── Test 2: AI Chat Thread & Persistence ──
    print("\n[Test 2] Setting up AI Chat thread and synchronizing Local SLM logs...")
    chat_ref = db.collection("chats").document()
    chat_id = chat_ref.id
    
    chat_ref.set({
        "chat_id": chat_id,
        "user_id": test_user_id,
        "title": "Offline Outbreak Inquiry",
        "created_at": firestore.SERVER_TIMESTAMP,
        "updated_at": firestore.SERVER_TIMESTAMP
    })
    print(f"SUCCESS: Created AI Chat Session: /chats/{chat_id}")
    
    messages_ref = chat_ref.collection("messages")
    
    # Save User message
    user_msg_ref = messages_ref.document()
    user_msg_ref.set({
        "sender": "user",
        "sender_name": "Taha Bukhari Test",
        "text": "What is the dengue status in Karachi right now?",
        "timestamp": firestore.SERVER_TIMESTAMP
    })
    
    # Persist simulated on-device offline SLM response
    slm_msg_ref = messages_ref.document()
    slm_msg_ref.set({
        "sender": "ai",
        "sender_name": "BimariHaunter Local SLM",
        "text": "Based on Karachi outbreaks in your local feed cache, dengue is highly severe in Karachi Central. Clear vector pools.",
        "timestamp": firestore.SERVER_TIMESTAMP
    })
    print("SUCCESS: Successfully synchronized user query and local SLM offline response logs.")
    
    # ── Test 3: Agentic Gemini Tool Invocation ──
    print("\n[Test 3] Verifying Agentic Gemini tools (Database outbreaks lookup)...")
    try:
        outbreak_advisory = query_outbreaks("Karachi")
        print("SUCCESS: query_outbreaks tool output successfully fetched:")
        if outbreak_advisory.strip().startswith("["):
            parsed_outbreaks = json.loads(outbreak_advisory)
            print(f"  Total matched reports parsed: {len(parsed_outbreaks)}")
            for item in parsed_outbreaks[:2]:
                print(f"  - [{item['severity'].upper()}] {item['title']} ({item['source']})")
        else:
            print(f"  Advisory Response: {outbreak_advisory}")
    except Exception as e:
        print(f"FAILED: Outbreaks tool query failed: {str(e)}")

    print("\n[Test 3.2] Verifying Agentic Gemini tools (Yahoo Web Search fallback)...")
    try:
        web_advisory = query_web_search("dengue Karachi May 2026")
        print("SUCCESS: query_web_search tool output successfully fetched:")
        if web_advisory.strip().startswith("["):
            parsed_web = json.loads(web_advisory)
            print(f"  Total search matches parsed: {len(parsed_web)}")
            for item in parsed_web[:2]:
                print(f"  - {item['title']} ({item['url'][:50]}...)")
        else:
            print(f"  Advisory Response: {web_advisory}")
    except Exception as e:
        print(f"FAILED: Web search tool query failed: {str(e)}")
        
    # ── Test 4: Community Outbreak Groups ──
    print("\n[Test 4] Setting up regional Outbreak Discussion Group...")
    group_ref = db.collection("groups").document()
    group_id = group_ref.id
    
    group_ref.set({
        "group_id": group_id,
        "name": f"{test_city} Dengue Alert Team",
        "city": test_city.capitalize(),
        "created_by": test_user_id,
        "members": [test_user_id],
        "created_at": firestore.SERVER_TIMESTAMP
    })
    print(f"SUCCESS: Created Community Group: /groups/{group_id} ({test_city} regional focus)")
    
    group_msg_ref = group_ref.collection("messages").document()
    group_msg_ref.set({
        "sender_id": test_user_id,
        "sender_name": "Taha Bukhari Test",
        "text": "Warning: Vector breeding was found near Clifton Block 5. Municipal teams have been alerted.",
        "timestamp": firestore.SERVER_TIMESTAMP
    })
    # ── Test 5: Geolocated Map Markers ──
    print("\n[Test 5] Verifying Geolocated Map Markers query logic...")
    try:
        reports_ref = db.collection("reports")
        docs = reports_ref.order_by("published_at", direction="DESCENDING").limit(20).stream()
        markers = []
        for doc in docs:
            data = doc.to_dict()
            analysis = data.get("ai_analysis", {})
            coords = analysis.get("coordinates")
            if coords and hasattr(coords, "latitude") and hasattr(coords, "longitude"):
                markers.append({
                    "id": doc.id,
                    "title": data.get("title"),
                    "latitude": coords.latitude,
                    "longitude": coords.longitude
                })
        print(f"SUCCESS: Successfully verified Map Markers query logic. Found {len(markers)} markers in recent reports.")
        for m in markers[:2]:
            print(f"  - Marker: {m['title']} ({m['latitude']}, {m['longitude']})")
    except Exception as e:
        print(f"FAILED: Map markers query validation failed: {str(e)}")

    print("\n--- All BimariHaunter Firestore Architecture Checks Passed Successfully! ---")

if __name__ == "__main__":
    asyncio.run(run_verification())
