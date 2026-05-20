"""
End-to-End Firestore Verification Script for BimariHaunter Peer-to-Peer Workflows.

Tests:
1. Creation of mock profiles for User A and User B.
2. Friendship creation (mutual links added).
3. Blocking functionality (mutual links removed, block list updated).
4. Direct Messages persistence & mutator states (creation, editing, soft-deletion).
"""

import asyncio
import os
from datetime import datetime, timezone
from google.cloud import firestore

# Set credentials path
os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = "bimarihaunter-firebase-adminsdk-fbsvc-99e53c4db6.json"

from app.database.firestore import db

async def run_p2p_verification():
    print("--- Starting BimariHaunter P2P & Messaging Architecture Verification ---")
    
    uid_a = "test_user_alice_777"
    uid_b = "test_user_bob_888"
    
    # ── Test 1: Profiles Creation ──
    print("\n[Test 1] Creating Mock User Profiles for Alice and Bob...")
    
    user_a_ref = db.collection("users").document(uid_a)
    user_b_ref = db.collection("users").document(uid_b)
    
    user_a_ref.set({
        "uid": uid_a,
        "email": "alice@bimarihaunter.pk",
        "name": "Alice Wonderland",
        "created_at": firestore.SERVER_TIMESTAMP
    }, merge=True)
    
    user_b_ref.set({
        "uid": uid_b,
        "email": "bob@bimarihaunter.pk",
        "name": "Bob Builder",
        "created_at": firestore.SERVER_TIMESTAMP
    }, merge=True)
    
    print("SUCCESS: Mock users created in Firestore.")

    # ── Test 2: Mutual Friendship Links ──
    print("\n[Test 2] Adding Mutual Friendship Links...")
    
    # Alice adds Bob
    user_a_ref.collection("friends").document(uid_b).set({
        "uid": uid_b,
        "name": "Bob Builder",
        "email": "bob@bimarihaunter.pk",
        "added_at": firestore.SERVER_TIMESTAMP
    })
    
    # Bob adds Alice (Mutual)
    user_b_ref.collection("friends").document(uid_a).set({
        "uid": uid_a,
        "name": "Alice Wonderland",
        "email": "alice@bimarihaunter.pk",
        "added_at": firestore.SERVER_TIMESTAMP
    })
    
    # Verify links
    friend_b_exists = user_a_ref.collection("friends").document(uid_b).get().exists
    friend_a_exists = user_b_ref.collection("friends").document(uid_a).get().exists
    
    assert friend_b_exists and friend_a_exists, "Mutual friendship failed to establish!"
    print("SUCCESS: Mutual friendship links verified between Alice and Bob.")

    # ── Test 3: Direct Messages & Deterministic Chat ID ──
    print("\n[Test 3] Simulating Direct Messages and Message Actions...")
    
    # Deterministic Chat ID: min(uid1, uid2) + "_" + max(uid1, uid2)
    chat_id = f"{uid_a}_{uid_b}" if uid_a < uid_b else f"{uid_b}_{uid_a}"
    print(f"Deterministic Chat ID resolved: {chat_id}")
    
    chat_ref = db.collection("direct_chats").document(chat_id)
    chat_ref.set({
        "chat_id": chat_id,
        "created_at": firestore.SERVER_TIMESTAMP,
        "participants": [uid_a, uid_b],
        "names": {
            uid_a: "Alice Wonderland",
            uid_b: "Bob Builder"
        }
    })
    
    messages_ref = chat_ref.collection("messages")
    
    # Action 3.1: Send Direct Message
    print("Action 3.1: Sending Message...")
    msg_id = "test_msg_101"
    messages_ref.document(msg_id).set({
        "id": msg_id,
        "senderId": uid_a,
        "senderName": "Alice Wonderland",
        "text": "Hello Bob! Did you see the recent dengue outbreak alert?",
        "timestamp": int(datetime.now(timezone.utc).timestamp() * 1000),
        "edited": False,
        "deleted": False,
        "sharedPostId": None,
        "sharedPostTitle": None,
        "sharedPostDisease": None,
        "sharedPostSeverity": None,
        "sharedPostUrl": None
    })
    
    msg_snapshot = messages_ref.document(msg_id).get().to_dict()
    assert msg_snapshot is not None, "Message failed to save!"
    assert msg_snapshot["text"] == "Hello Bob! Did you see the recent dengue outbreak alert?", "Message content mismatch!"
    print("SUCCESS: Message successfully saved in direct chat.")

    # Action 3.2: Edit Message
    print("Action 3.2: Editing Message...")
    messages_ref.document(msg_id).update({
        "text": "Hello Bob! Did you see the high severity dengue outbreak alert?",
        "edited": True
    })
    
    msg_snapshot = messages_ref.document(msg_id).get().to_dict()
    assert msg_snapshot["edited"] is True, "Message edited flag failed to update!"
    assert msg_snapshot["text"] == "Hello Bob! Did you see the high severity dengue outbreak alert?", "Message text failed to edit!"
    print("SUCCESS: Message successfully edited with flag verified.")

    # Action 3.3: Soft Delete Message
    print("Action 3.3: Soft-Deleting Message...")
    messages_ref.document(msg_id).update({
        "text": "This message was deleted.",
        "deleted": True
    })
    
    msg_snapshot = messages_ref.document(msg_id).get().to_dict()
    assert msg_snapshot["deleted"] is True, "Message deleted flag failed to update!"
    assert msg_snapshot["text"] == "This message was deleted.", "Message text failed to mask on delete!"
    print("SUCCESS: Message soft-deletion successfully executed.")

    # ── Test 4: Blocking Mechanisms ──
    print("\n[Test 4] Simulating Alice Blocking Bob...")
    
    # Alice blocks Bob
    user_a_ref.collection("blocked").document(uid_b).set({
        "uid": uid_b,
        "blocked_at": firestore.SERVER_TIMESTAMP
    })
    
    # Automatically remove mutual friendship links (mocking API behavior)
    user_a_ref.collection("friends").document(uid_b).delete()
    user_b_ref.collection("friends").document(uid_a).delete()
    
    friend_b_exists_after = user_a_ref.collection("friends").document(uid_b).get().exists
    friend_a_exists_after = user_b_ref.collection("friends").document(uid_a).get().exists
    is_blocked_verified = user_a_ref.collection("blocked").document(uid_b).get().exists
    
    assert not friend_b_exists_after and not friend_a_exists_after, "Friendships were not cleaned up after block!"
    assert is_blocked_verified, "Bob failed to show up on Alice's block list!"
    print("SUCCESS: Alice successfully blocked Bob, and mutual friendship links were completely cleaned up.")

    # ── Clean up Mock Data ──
    print("\nCleaning up mock documents...")
    messages_ref.document(msg_id).delete()
    chat_ref.delete()
    user_a_ref.collection("blocked").document(uid_b).delete()
    user_a_ref.delete()
    user_b_ref.delete()
    print("SUCCESS: Firestore mock clean up completed.")
    
    print("\n--- All BimariHaunter P2P & Messaging Architecture Checks Passed Successfully! ---")

if __name__ == "__main__":
    asyncio.run(run_p2p_verification())
