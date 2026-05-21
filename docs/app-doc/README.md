# BimariHaunter — app doc

Quick, focused notes for developers and designers.

- What: Android app that ingests news, classifies outbreaks, and surfaces proximity alerts.
- Core assets: `docs/app-doc/images/` (ghost mascots and stickers).

Quick setup:
1. Android: open `android/` in Android Studio and run.
2. Backend: see `bimari-haunter/backend/README` for deploy and API info.

Need details? The long-form docs are in this repo — use them for architecture, scraping rules, NLP models and security rules.

Contribute: open issues or PRs; keep changes small and documented.

---

## Setup & Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- A Firebase project with Firestore, Auth, and FCM enabled
- Google Maps API key

### Steps

1. Clone the repository and open it in Android Studio.
2. Place your `google-services.json` in `app/`.
3. Add your Google Maps API key to `local.properties`:
   ```
   MAPS_API_KEY=your_key_here
   ```
4. (Optional) Place a compatible MediaPipe `.task` model file at `app/src/main/assets/models/llama-3.2-1b-instruct.task` to enable the offline AI assistant.
5. Sync Gradle and run on a device or emulator with Google Play Services.

---

## Firebase Configuration

### Firestore Security Rules

Apply the following rules in the Firebase console under **Firestore → Rules**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /reports/{doc} {
      allow read: if true;
      allow write: if false;
    }

    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
      match /friends/{doc}   { allow read, write: if request.auth != null && request.auth.uid == userId; }
      match /blocked/{doc}   { allow read, write: if request.auth != null && request.auth.uid == userId; }
    }

    match /chats/{chatId} {
      allow read: if true;
      allow write: if request.auth != null;
      match /messages/{msgId} { allow read: if true; allow write: if request.auth != null; }
    }

    match /groups/{doc}           { allow read: if true; allow write: if request.auth != null; }
    match /direct_chats/{chatId}  { allow read, write: if request.auth != null;
      match /messages/{msgId}     { allow read, write: if request.auth != null; } }
    match /friend_requests/{doc}  { allow read, write: if request.auth != null; }
    match /scrape_jobs/{doc}      { allow read: if request.auth != null; allow write: if false; }
  }
}
```

> **Important:** The default rules shipped with new Firestore projects are `allow read, write: if false`. This blocks everything. The rules above must be published before the app can read any data.

---

## Backend

The backend is a Python service deployed on Google Cloud Run at:

```
https://bimari-haunter-723264184490.asia-southeast1.run.app/
```

It exposes a `POST /trigger-job` endpoint that the Android app calls (with a Firebase ID token) to request an immediate scrape cycle. The scraper fetches RSS feeds from Pakistani and international health news sources, runs each article through a keyword classifier (`rss-keyword-classifier-v2`) to detect disease mentions and severity, extracts coordinates via a geocoding step, and writes the result to `/reports` in Firestore.

The Android app authenticates all backend requests through `AuthInterceptor`, which calls `FirebaseAuth.currentUser.getIdToken(true)` on a background thread and injects the resulting JWT as a `Bearer` token.

---

## Notification Channels

| Channel ID | Name | Purpose |
|-----------|------|---------|
| `outbreak_alerts` | Outbreak Alerts | Proximity-triggered disease warnings |
| `messages` | Messages | Direct and group chat messages |
| `friend_requests` | Friend Requests | New connection requests |
| `location_alerts` | Location Alerts | City-change detection |

---

## Ghost Mascot System

BimariHaunter features a ghost mascot character that appears throughout the app as contextual stickers. The ghost communicates app state without relying on text alone.

| Asset | Emotion | Where it appears |
|-------|---------|-----------------|
| `ghost_happy` | Happy | App icon, splash screen |
| `ghost_waving` | Welcoming | AddFriends empty state |
| `ghost_thinking` | Thinking | Insights loading state |
| `ghost_sleep` | Sleeping | Feed loading state |
| `ghost_sad` | Sad | Feed empty/no-results state |
| `ghost_alert` | Alert | 15km proximity banner on map |
| `ghost_hero` | Heroic | Onboarding and auth screens |

---

## Contributing

This project was built for a hackathon. If you want to extend it, the most impactful areas are:

- **Expanding the scraper** to cover more RSS sources and languages (Urdu support is a priority).
- **Improving the AI classifier** — the current `rss-keyword-classifier-v2` is keyword-based; a fine-tuned transformer would significantly improve precision.
- **Adding heatmap layers** to the map for visualising outbreak density.
- **Implementing the Alerts tab** with user-configurable disease and location filters.
- **Internationalisation** — the UI is English-only; Urdu RTL support would dramatically expand the user base.
