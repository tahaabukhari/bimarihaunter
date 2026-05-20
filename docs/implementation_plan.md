# BimariHaunter Architecture & Firebase Implementation Plan

## Architectural Overview
This plan focuses entirely on bridging your two primary codebases:
1. **The Python Backend** (FastAPI / Scraper)
2. **The Android Native Frontend** (Kotlin / Android Studio)

Because you do not have access to your existing Google Cloud environment to manage Cloud SQL, we are shifting to **Firebase / Cloud Firestore** as our primary database. Firestore is fully managed, NoSQL, and incredibly easy to access directly from the Firebase Console without needing complex GCP networking. 

We will update the backend to serve data via REST and wire up the native Android Kotlin app to consume it and handle authentication.

## Proposed Changes

### 1. Firebase Setup & Configuration (Manual Step)
Since we are using Firebase for both Auth and the Database, you will need to:
- Create a Firebase Project in the [Firebase Console](https://console.firebase.google.com/).
- Enable **Google Sign-In** as an authentication provider.
- Enable **Firestore Database** in test mode (or production mode with basic rules).
- Generate a **Service Account key (JSON)** for the FastAPI backend and place it in the `backend/` folder (e.g., `firebase-service-account.json`).

### 2. Backend (FastAPI) Changes

#### [MODIFY] `requirements.txt`
- Add `firebase-admin` and `google-cloud-firestore`.
- Remove PostgreSQL dependencies (`asyncpg`, `sqlalchemy`, `alembic`) since we are moving to Firestore.

#### [NEW] `app/database/firestore.py`
Create a centralized Firestore client initialization using the Service Account key.

#### [MODIFY] Existing Models & Routes
- Refactor our data schemas (Users, Reports, Jobs) from SQLAlchemy models to Pydantic models designed for Firestore document storage.
- **Routing**: Create a dedicated `GET /api/v1/feed` endpoint. This route will query Firestore for the latest scraped reports and return them as JSON to feed the Android app.

#### [NEW] `app/services/firebase_auth.py`
Create a FastAPI dependency to verify the Authorization Bearer token (JWT) passed from the Android app using `firebase_admin.auth.verify_id_token`.

### 3. Android Native App (Kotlin) Changes

#### [MODIFY] `build.gradle.kts` (Project & App levels)
- Add the `com.google.gms.google-services` plugin and Firebase BOM dependencies.
- Add Retrofit and OkHttp dependencies for consuming the FastAPI REST endpoints.

#### [NEW] `google-services.json`
Place the configuration file in the `app/` directory (you will get this from the Firebase Console).

#### [NEW] Native UI & Logic
- **Authentication**: Implement Google Sign-In using the Android Credential Manager. Extract the Firebase ID Token and securely store it (e.g., using EncryptedSharedPreferences) to inject into the `Authorization` headers for Retrofit API calls.
- **Feed UI**: Build out the RecyclerView and Adapters to fetch data from the FastAPI `GET /api/v1/feed` route and display it to the user.

## User Review Required

> [!WARNING]  
> Migrating away from PostgreSQL to Firestore means we are changing the core data storage mechanism in the Python backend. This will require refactoring the existing SQLAlchemy code. 
> Additionally, the Android project is currently an empty shell (no Kotlin UI or logic exists yet). We will be building the Kotlin screens and Networking (Retrofit) from scratch.

## Open Questions

1. **Firebase Project**: Can you go to [console.firebase.google.com](https://console.firebase.google.com/), create a project, and enable **Authentication** and **Firestore**?
2. **Service Account Key**: Once created, can you generate a Service Account key (Project Settings -> Service Accounts -> Generate New Private Key) and place it in the backend folder (e.g., `bimari-haunter/backend/firebase-key.json`)? Let me know the exact filename.
3. **Redeployment**: Since you lost access to the deployed backend on Google Cloud, we will have to redeploy this updated version once we finish. Do you want to deploy it using the new Firebase project (which creates a new GCP project behind the scenes) using Cloud Run? 

Once you approve this plan, I'll start by stripping out the PostgreSQL code from the FastAPI backend and setting up the Firestore client!
