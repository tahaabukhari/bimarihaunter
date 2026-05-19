# Phase 1: Deployment & Full-Stack Integration Plan

This plan documents the end-to-end integration of the BimariHaunter ecosystem, encompassing backend cloud deployment, Android app authentication (Google + Email/Password), Firestore database migration, and the foundation for real-time chat.

## 1. Cloud Run Deployment Fixes (Python Backend)
The current `Dockerfile` downloads heavy ML models (`facebook/bart-large-mnli`, `facebook/bart-large-cnn`, and `en_core_web_trf`) during the build step. Cloud Build often times out on these huge downloads, and Cloud Run struggles to start the container quickly (Cold Start timeouts) if it has to load these massive models into RAM instantly.

**Steps to Fix:**
1. **[MODIFY] `Dockerfile`**: We will wrap the HuggingFace downloads in a robust python script (`scripts/download_models.py`) with retry logic instead of the brittle inline `python -c` command.
2. **Increase Cloud Run Memory/Timeout**: When you run the deploy script, we must explicitly allocate at least `4Gi` (or `8Gi`) of memory and increase the startup timeout so the container doesn't crash before Uvicorn starts.

## 2. Authentication & Database (Firebase/Firestore)
We will use Firebase as the unified solution for Auth and Data storage, accessible by both the Android App and the Python Backend.

**Steps for Android (Kotlin):**
1. **Setup Firebase**: Add `google-services.json` and Firebase BOM to `build.gradle.kts`.
2. **Auth UI**: Create a Login screen in Kotlin supporting both **Google Sign-In** (Credential Manager) and standard **Email/Password**.
3. **Auth State**: Save the Firebase JWT token locally so Retrofit can inject it into headers for backend calls.

**Steps for Python Backend (FastAPI):**
1. **[MODIFY] `requirements.txt`**: Add `firebase-admin` and `google-cloud-firestore`.
2. **Firestore Migration**: Refactor backend models to save scraped disease data directly to Firestore collections (`/reports`, `/jobs`) instead of PostgreSQL.
3. **Auth Dependency**: Create a FastAPI dependency (`app/services/firebase_auth.py`) that uses `firebase-admin` to verify the JWT sent from the Android app.

## 3. Real-Time Chat & Group Chats (Tech Stack Selection)
For building real-time chats, we have two viable options given our current stack:
- **Option A (WebSockets + Redis)**: Use FastAPI WebSockets backed by Google Cloud Memorystore (Redis) to broadcast messages between instances. (This is highly complex to scale serverlessly).
- **Option B (Firestore Realtime Listeners)**: Since we are already moving to Firestore, we can use **Firestore Snapshot Listeners** directly in the Android app. 

**Recommendation: Option B (Firestore)**
Firestore was built exactly for this. We can create a `/chats` collection in Firestore. 
- The Android app listens directly to this collection for new messages (zero latency, offline support built-in).
- The Python Backend only gets involved if we need to run NLP analysis (e.g., detecting disease keywords in user chats) or moderation on the messages.

**Implementation Steps:**
1. **Firestore Schema**: Define schemas for `/conversations`, `/conversations/{id}/messages`.
2. **Android UI**: Build standard RecyclerView chat interfaces in Kotlin.
3. **Android Listeners**: Attach `addSnapshotListener` to the messages collection for real-time updates.

---

## Your Action Items (Step-by-Step Guide for YOU)

Before I can start coding this out, you need to complete these configuration steps:

1. **Go to [Firebase Console](https://console.firebase.google.com/)** and create a new project called `BimariHaunter`.
2. **Setup Authentication**:
   - Go to Build -> Authentication -> Get Started.
   - Enable **Google Provider**.
   - Enable **Email/Password Provider**.
3. **Setup Firestore Database**:
   - Go to Build -> Firestore Database -> Create Database.
   - Start in **Test Mode** (we will secure it later).
4. **Get Backend Credentials**:
   - Go to Project Settings (gear icon top left) -> Service Accounts.
   - Click **Generate New Private Key**. 
   - Save the downloaded JSON file as `firebase-service-account.json` and place it inside `c:\Users\S-Z Computers\bimarihaunter-backend\bimari-haunter\backend`.
5. **Get Android Credentials**:
   - Go to Project Settings -> General.
   - Scroll down to "Your apps" and click the Android icon to add an app.
   - Enter your package name: `com.example.bimarihaunter`
   - Download the `google-services.json` file.
   - Place it inside `c:\Users\S-Z Computers\AndroidStudioProjects\bimarihaunter\app`.

## User Review Required

> [!IMPORTANT]  
> Please review the tech stack choice for Chat (using Firestore directly vs FastAPI WebSockets). Firestore is objectively the industry standard for this use case and will save us weeks of development time. 

Let me know once you approve this plan and have completed your action items (placing the two `.json` files in their respective folders)!
