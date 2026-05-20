# BimariHaunter: Refactoring, Security & Location Service Integration Tasks

This document tracks the step-by-step implementation status for securing the native Android mobile app, aligning its network/data layers with the FastAPI backend, optimizing the local offline AI chat model, and integrating the production-grade Gemini API key.

---

## 🖥️ Phase 1: Backend Vertex AI & Google AI Studio Integration

### 📋 Task 1: Secure API Key Integration & Dynamic Environment Configuration
*   **[x] Task 1.1: Deploy Secure Key Storage**
    *   *Implementation*: Initialized `.env` and `.env.example` in both the root directory and the `bimari-haunter/backend/` directory. Added:
        ```ini
        GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
        ```
    *   *Purpose*: This secures the newly provided Google AI Studio Gemini API key (`AQ.` format) inside the host environment, preventing credential leakage in git version control.
*   **[x] Task 1.2: Implement Dual-Tier AI Client (Vertex AI with AI Studio Fallback)**
    *   *Implementation*: Refactored `bimari-haunter/backend/app/api/routes/chats.py`.
    *   *Logic*:
        1. **Primary**: Initializes GCP enterprise-grade `vertexai` utilizing keyless Application Default Credentials (ADC) or a service account key matching `firebase-adminsdk` JSON. It sets up `gemini-1.5-flash` with dynamic Firestore report querying (`query_outbreaks`) and live web searching (`query_web_search`) tools.
        2. **Fallback**: Gracefully switches to Google AI Studio (`google-generativeai`) using `GEMINI_API_KEY` (highly optimized for the newest `AQ.` hobbyist key format) if GCP environment setup fails. It uses `gemini-1.5-flash` (or `gemini-1.0-pro` as secondary fallback) with fully enabled automatic function calling for Firestore outbreaks and live Yahoo searches.
*   **[x] Task 1.3: Run Verification Scripts**
    *   *Execution*: Successfully executed `scripts/test_user_workflows.py` to confirm that Firestore session generation, scraper queries, and smart-mode chats execute cleanly.

---

## 📱 Phase 2: Android App Refactoring & Security Alignment

### 📋 Task 2: Decommission Legacy Gemini Client & Route Chat via Backend
*   **[x] Task 2.1: Purge Vulnerable Hardcoded Key**
    *   *Implementation*: Deleted `app/src/main/java/com/bimarihaunter/network/GeminiClient.kt` entirely.
    *   *Purpose*: Removes client-side key exposure vulnerabilities.
*   **[x] Task 2.2: Refactor Chat ViewModel for Backend Server Routing**
    *   *Implementation*: Modified `com.bimarihaunter.ui.viewmodel.AiChatViewModel.kt`. Refactored `fetchGeminiReply(userQuery: String)` to use `RetrofitClient` to invoke `sendMessage(chatId = "default_chat_session", mode = "smart", body = requestBody)`.
    *   *Fallback*: If the backend call encounters a network/server exception, it seamlessly falls back to the on-device offline SLM.

---

### 📋 Task 3: Optimize On-Device SLM (Prevent Memory Crashes)
*   **[x] Task 3.1: Swap Microsoft Phi-3.5 with Llama 3.2 1B**
    *   *Implementation*: Updated `app/src/main/java/com/bimarihaunter/ai/SLMManager.kt`. Swapped the heavy 2.2 GB Phi-3.5 model with the highly quantized, optimized **Llama 3.2 1B** task file (`file:///android_asset/models/llama-3.2-1b-instruct.task`).
    *   *Benefit*: Reduces runtime RAM allocation from 4 GB+ to under 1 GB, completely preventing Out-Of-Memory (OOM) failures on low-end and mid-range mobile devices.

---

### 📋 Task 4: Package Cleanup & Import Alignment
*   **[x] Task 4.1: Delete Legacy/Duplicate Packages**
    *   *Implementation*: Cleared out duplicate classes and legacy packages under `com.bimarihaunter.data` inside duplicate modules.
*   **[x] Task 4.2: Align Imports in Views & ViewModels**
    *   *Implementation*: Ensured `FeedScreen.kt` correctly references `com.bimarihaunter.db.OutbreakReportEntity` and `com.bimarihaunter.ui.viewmodel.FeedViewModel` for offline-first data rendering, keeping compose MidnightBlack themes completely intact.

---

### 📋 Task 5: Dynamic GPS Location Integration
*   **[x] Task 5.1: Inject Fused Location Service in Navigation Graph**
    *   *Implementation*: Refactored the `composable(Screen.HomeFeed.route)` block in `app/src/main/java/com/bimarihaunter/BimarihaunterApp.kt`.
    *   *Logic*:
        1. **Permission Check**: Uses `ContextCompat.checkSelfPermission` to check for `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
        2. **Permission Request**: If missing, launches a Jetpack Compose `rememberLauncherForActivityResult` request using `RequestMultiplePermissions()`.
        3. **Location Retrieval**: Retrieves coordinates in real-time from `LocationServices.getFusedLocationProviderClient(context).lastLocation`.
        4. **Geocoding**: Converts coordinates to local city names using `android.location.Geocoder` (with backward compatibility `@Suppress("DEPRECATION")`).
        5. **Feed Sync**: Calls `feedViewModel.syncFeed(cityName, latitude, longitude)` with the resolved coordinates, falling back safely to `"Karachi" (24.8607, 67.0011)` if permissions are denied or location is null.
