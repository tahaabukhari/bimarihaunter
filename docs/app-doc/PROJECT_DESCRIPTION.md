# BimariHaunter — Project Description

## The Problem

Pakistan experiences recurring outbreaks of dengue, cholera, typhoid, polio, and other preventable diseases. When an outbreak begins, the information gap between health authorities and ordinary citizens can span days or weeks. By the time a warning reaches the public through traditional media, the disease has often already spread beyond the initial cluster.

There is no consumer-facing tool in Pakistan that aggregates real-time disease intelligence, maps it geographically, and delivers it to the person most at risk — the individual who happens to be in or near the affected area right now.

## The Solution

BimariHaunter is a real-time public health surveillance platform consisting of two components: a cloud-based AI scraper that continuously monitors health news sources, and an Android application that puts the resulting intelligence directly in the user's hands.

The name "BimariHaunter" is a blend of the Urdu word *bimari* (disease/illness) and the English word *haunter* — the app hunts diseases before they can hunt you.

## How It Works

A Python scraper deployed on Google Cloud Run polls RSS feeds from Pakistani and international health news outlets on a continuous cycle. Each article is passed through a keyword classification model (`rss-keyword-classifier-v2`) that identifies disease mentions, assigns a severity rating (low, medium, or high), extracts location references, and geocodes them to latitude/longitude coordinates. The classified report is then written to a Firestore document in the `/reports` collection.

The Android app maintains a persistent Firestore listener on `/reports`. New documents arrive in real-time and are cached in a local Room database. The UI reads exclusively from the local cache, which means the feed loads instantly and continues to function when the device is offline.

When the user opens the map, outbreak pins are rendered dynamically as they pan and zoom — only the markers within the current viewport are drawn, keeping the experience smooth regardless of how many reports exist in the database. Each pin shows the disease name and a human-readable timestamp indicating when the data was last refreshed.

The most distinctive feature is **proximity alerting**. As the user moves through the city, the app continuously compares their GPS coordinates against the coordinates of every cached outbreak report using the Haversine formula. If they come within 15 kilometres of a known outbreak, a ghost-branded alert banner slides in from the top of the map screen and a system notification fires simultaneously. The same check runs passively in the background during travel.

## Key Features at a Glance

**Live Feed** — Real-time disease reports from across Pakistan, filterable by disease type, severity, and location. Cached locally for offline access.

**Interactive Map** — Google Maps integration with dynamic viewport-based pin rendering. Pins load as you explore, with severity-coded colours (green/amber/red) and last-updated timestamps.

**Proximity Alerts** — Automatic 15km radius notifications when the user enters an outbreak zone, both as an in-app banner and a system push notification.

**Live Insights** — A dedicated analytics tab showing Today's Story (the most critical active outbreak), a disease breakdown chart, severity distribution, and a reports-per-day trend — all computed from live data, no static values.

**Community Chat** — Public group chats for outbreak discussions, direct messaging between friends, and an AI-powered chat assistant that answers health queries using locally cached data when offline.

**Friend System** — Add friends by scanning their QR code or entering their UID directly. Each user profile generates a unique QR code for easy sharing.

**Offline AI Assistant** — A quantised Llama 3.2 1B model runs entirely on-device via MediaPipe LLM Inference. When there is no internet connection, the assistant answers health questions using only the locally cached outbreak reports as context, ensuring it never hallucinates information it does not have.

## Impact

BimariHaunter addresses a genuine public health communication gap in a country of 230 million people. The core data pipeline is already live and populating Firestore with real reports. The proximity alert system means that a user travelling from Lahore to a dengue-affected district will receive a warning before they arrive — not after they have already been exposed.

The ghost mascot character — appearing as contextual stickers throughout the app — gives the product a distinctive identity that makes a serious subject approachable, particularly for younger users who are most likely to adopt a new health application.

## Technology Choices

The decision to use Firestore as the primary data store rather than a traditional REST API was deliberate. Firestore's real-time listeners mean that new outbreak reports appear on every connected device within seconds of the scraper writing them, without any polling. The local Room cache means the app is resilient to network interruptions. The combination gives BimariHaunter the responsiveness of a live dashboard with the reliability of an offline-first application.

The on-device AI model was chosen specifically to handle the scenario where a user is in a rural area with poor connectivity — precisely the scenario where health information is most scarce and most needed.

## Team

Built at a hackathon. The backend scraper, Firebase infrastructure, Android application, AI integration, and design system were all developed within the competition window.
