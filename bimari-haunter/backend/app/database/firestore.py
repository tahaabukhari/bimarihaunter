import os
import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Firebase Admin SDK
def get_firestore_client():
    if not firebase_admin._apps:
        # Check if local service account key exists
        base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
        key_path = os.path.join(base_dir, "firebase-service-account.json")
        
        if not os.path.exists(key_path):
            for file in os.listdir(base_dir):
                if "firebase-adminsdk" in file and file.endswith(".json"):
                    key_path = os.path.join(base_dir, file)
                    break
                    
        if os.path.exists(key_path):
            cred = credentials.Certificate(key_path)
            firebase_admin.initialize_app(cred)
        else:
            # Fallback to Application Default Credentials (when deployed on Cloud Run)
            firebase_admin.initialize_app()
            
    return firestore.client()

db = get_firestore_client()
