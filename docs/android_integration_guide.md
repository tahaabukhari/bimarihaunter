# BimariHaunter: Android Native Kotlin Integration Guide for Antigravity AI

Hello Antigravity (Android Agent)! This guide contains the exact backend API contracts, Firestore database schemas, and step-by-step implementation tasks required to build the Kotlin-based Android application for BimariHaunter. 

---

## 1. System Architecture Overview

BimariHaunter utilizes a **Dual-Mode AI Outbreak Detection and Consultation** architecture:
* **The Feed (Offline RAG Source)**: The backend serves a highly personalized feed, limited to **exactly 50 items**, containing the freshest news, X (Twitter), and Facebook public updates within the user's live city.
* **Offline SLM Mode (Local Chat)**: The Android app caches these 50 posts inside a local **SQLite Room Database**. For basic offline inquiries, the app prompts an **on-device SLM** (using MediaPipe or ML Kit) using these cached database records as reference context.
* **Smart Mode (Cloud Agent Chat)**: When toggled to "Smart Mode," the app hits the backend FastAPI server, which invokes a **Google Gemini Agent** running active tools to search the global database and scrape Yahoo/DuckDuckGo web results.

---

## 2. Backend API Contracts

All endpoints are prefixed with `/api/v1` and require a **Firebase Auth ID Token** in the `Authorization: Bearer <ID_Token>` header.

### 2.1 Update Location & Sync Feed
* **Endpoint**: `POST /api/v1/users/location`
* **Request Header**: `Authorization: Bearer <Firebase_ID_Token>`
* **Request Body** (`application/json`):
```json
{
  "city": "Karachi",
  "latitude": 24.8607,
  "longitude": 67.0011
}
```
* **Response Body** (`200 OK`):
```json
{
  "status": "success",
  "message": "Successfully updated location to Karachi and synchronized 50 local feed posts.",
  "city": "Karachi",
  "feed_count": 50
}
```

### 2.2 Get Personalized Feed
* **Endpoint**: `GET /api/v1/feed`
* **Request Header**: `Authorization: Bearer <Firebase_ID_Token>`
* **Query Params**: `limit=50`
* **Response Body** (`200 OK` - returns a List of matching reports):
```json
[
  {
    "id": "sha256_deterministic_article_hash",
    "title": "Dengue spike reported in Gulshan Karachi",
    "source": "Dawn News",
    "url": "https://dawn.com/article/dengue-karachi",
    "raw_text": "Full text snippet goes here...",
    "published_at": "2026-05-20T02:45:00Z",
    "scraped_at": "2026-05-20T03:00:00Z",
    "status": "analyzed",
    "source_type": "web",
    "ai_analysis": {
      "disease": "dengue",
      "severity": "high",
      "summary": [
        "Karachi Central has reported a 40% increase in dengue cases.",
        "Stagnant municipal water identified as vector source."
      ],
      "symptoms": ["fever", "joint pain", "rash"],
      "locations": ["Karachi", "Gulshan"],
      "coordinates": {
        "latitude": 24.8607,
        "longitude": 67.0011
      },
      "confidence_score": 0.89,
      "model_used": "facebook/bart-large-mnli"
    }
  }
]
```

### 2.3 Send Direct Chat Message (Local / Smart Mode)
* **Endpoint**: `POST /api/v1/chats/{chatId}/messages`
* **Query Param**: `mode` (either `local` or `smart`)
* **Request Header**: `Authorization: Bearer <Firebase_ID_Token>`
* **Request Body** (`application/json`):
```json
{
  "text": "What are the latest dengue cases near Karachi Central?",
  "local_slm_response": "Simulated or on-device compiled answer..." // Required ONLY when mode=local
}
```
* **Response Body** (`200 OK`):
```json
{
  "status": "success",
  "mode": "smart",
  "user_message_id": "doc_id_user_msg",
  "ai_message_id": "doc_id_ai_msg",
  "response": "Gemini synthesized answer detailing current Karachi Central cases using dynamic tools..."
}
```

---

## 3. Step-by-Step Android Development Checklist

### 📋 Task 1: Networking & Firebase Auth Setup
1. **Retrofit Configuration**: Build a single Retrofit client instance targeting `https://<YOUR_CLOUD_RUN_URL>/api/v1/`.
2. **Auth Header Interceptor**: Create an OkHttpClient `Interceptor` that dynamically fetches the current Firebase User's token and attaches it to all requests:
```kotlin
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser
        val token = user?.getIdToken(false)?.result?.token // Fetch synchronously if possible
        
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
```

### 📋 Task 2: Offline-First Room DB Feed Cache
1. **Room Entities**: Create a `OutbreakReport` Room entity mirroring the feed JSON structure (specifically extracting `title`, `source`, `published_at`, `disease`, `severity`, `summary`, and coordinates).
2. **Local Repository Cache**: 
   * When fetching `GET /api/v1/feed`, write the items into the Room DB.
   * Restrict local cached records to the **freshest 50 items** using a database query that trims older records.
   * Bind the Room DB data flow to the UI via `Flow<List<OutbreakReport>>` so that offline caching is completely seamless to the user.

### 📋 Task 3: Client-Side SLM (Offline Local RAG)
1. **Add On-Device LLM**: Integrate **MediaPipe LLM Inference API** or **ML Kit** to run a lightweight model on the phone (e.g., LLaMA-3.2-1B, Gemma-2B, or Phi-3).
2. **Offline Prompt Compiler (RAG)**: 
   * When the user inputs an inquiry while in **Local Mode (Offline)**, query the local Room database to compile the context text.
   * Format the final prompt to the local SLM as follows:
```
System: You are an offline outbreak assistant. Speak ONLY using the local feed context below.
Context:
- [Dawn News] Karachi dengue spike reported. Symptoms: fever. Severity: High.
- [X Post] Malaria cases rising in Gulshan.
User Query: Are there dengue outbreaks in Karachi?
AI Answer:
```
3. **Persist History**: Once a response is generated, enqueue a worker to sync this chat message log with the backend `/messages` route with `mode=local` once internet connectivity is restored.

### 📋 Task 4: UI Outbreak Datamap (Google Maps Integration)
1. **Google Maps SDK**: Setup Google Maps fragment inside the application.
2. **Custom Markers**: Parse the geocoded coordinate markers returned inside `ai_analysis.coordinates` in the personalized feed.
3. **Visual Cues**: Color-code marker pins based on report severity:
   * **Red Marker**: `severity == "high"`
   * **Orange/Yellow Marker**: `severity == "medium"`
   * **Green Marker**: `severity == "low"`
4. **Outbreak Detail Drawer**: Display a sliding custom sheet showing the AI-generated `summary` bullet points, source link, and diagnosed disease.

### 📋 Task 5: Firebase Cloud Messaging (FCM)
1. **FCM Service**: Extend `FirebaseMessagingService` to capture background pushes.
2. **Geofenced Notifications**: When an FCM message containing coordinate details arrives, check if the outbreak falls within a 10km radius of the user's current live location. If yes, display a high-severity alert notification immediately!

---

Good luck, Antigravity! All APIs, Firestore schemas, and RAG databases are completely finalized and verified on the server. You are ready to start building!
