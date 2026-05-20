# Disease Outbreak Feed Integration

Implementation plan to build the Android application feed features that fetch news reports from Google Firestore via a FastAPI backend, supporting real-time search and disease/severity filters.

## Overview
This feature integrates Retrofit to pull outbreak reports from a FastAPI backend. It utilizes OkHttp interceptors to attach Firebase Auth credentials for authentication, and displays the articles dynamically in a new Jetpack Compose screen with state-management, client-side filtering, and keyword search.

## Proposed Changes

### Data Models
- **Report, AiAnalysis, Coordinates**: Represents the reports schema fetched from backend. Defined in `com.bimarihaunter.data.model.Report.kt`.

### Network Layer
- **AuthInterceptor**: Injects current Firebase User ID tokens into the requests as `Bearer` header authentication.
- **ApiService**: Direct declaration of Retrofit API endpoints.
- **NetworkClient**: Builds Retrofit instance using Gson and customized OkHttpClient.

### Repository Layer
- **FeedRepository**: Interfaces networking layer with business viewmodels.

### ViewModel Layer
- **FeedUiState**: Defines ui rendering states (`Loading`, `Success`, `Error`, `Empty`).
- **FeedViewModel**: Observes and updates the feed list on query/filters.

### UI Layer
- **FeedScreen**: Jetpack Compose implementation of the list, chips, search bar, and badges.
- **BimarihaunterApp**: Redirects `Screen.HomeFeed.route` to `FeedScreen`.
