# Playwright Social Media Scrapers & Regional Datamap Integration Plan

## Goal
To build resilient, anonymous public scrapers for **X (Twitter)** and **Facebook** using Playwright, completely bypassing expensive, restrictive official APIs. These scrapers will filter health posts by location in real-time, feeding geocoded outbreak data into a Firestore-backed **Regional Datamap** for the Android app.

---

## User Review Required

> [!IMPORTANT]  
> **Anonymous Scraping Obscurity (Bypassing Login Blocks)**: 
> * **X (Twitter)** aggressively redirects anonymous search queries to a login wall. To scrape X without requiring a burner account (which gets banned instantly), we will route queries through **Nitter** (a lightweight, open-source public Twitter mirror). Nitter has zero javascript obfuscation and zero login walls.
> * **Facebook** also pushes login prompts on standard web pages. We will bypass this by scraping the lightweight mobile interface **`mbasic.facebook.com`** via Playwright, which is fast and rarely blocks anonymous public searches.

---

## Proposed Changes

### 1. `app/scraper/social_playwright.py` [NEW]
We will create a new social media scraper module leveraging Playwright's asynchronous API.

* **`XScraper` Class**:
  - Targets Nitter search endpoints (e.g., `https://nitter.poast.org/search?f=tweets&q={query}`).
  - Scrapes matching tweets, text, timestamps, permalinks, and media.
  - Automatically rotates between active public Nitter instances if one fails.
* **`FacebookPlaywrightScraper` Class**:
  - Targets `https://mbasic.facebook.com/search/posts/?q={query}`.
  - Automates crawling of public status updates without logging in.
  - Extracts text, links, and posting dates.
* **Location Resolution**:
  - Parses posts for location keywords (Karachi, Lahore, etc.).
  - Binds verified GeoPoint coordinates directly to each post.

### 2. `app/scraper/scheduler.py` [MODIFY]
We will update the scheduler to execute our new Playwright social scrapers:
- Define default search queries (e.g., `"dengue pakistan"`, `"malaria karachi"`, `"cholera lahore"`).
- Remove references to `FacebookGraphClient`.
- Run the `NLPProcessor` on extracted social posts to run our classification and entity matching.
- Save enriched posts directly to `/reports` in Firestore using content hashes as IDs.

### 3. Firestore Schema Adjustments [MODIFY]
Since social media posts are typically shorter and more regional than news articles, we will enrich the `/reports` Firestore collection structure:
- **`source_type`**: `"web"` or `"social"` (helps frontend filter news vs. social feeds).
- **`platform`**: `"x"` or `"facebook"` (for social media posts).
- **`ai_analysis.locations`**: Array of resolved cities.
- **`ai_analysis.coordinates`**: Firestore GeoPoint matching the resolved location (used to plot heatmaps on the Android regional datamap).

---

## Open Questions

1. **Scraping Frequency**: Social media moves fast. Shall we configure these scrapers to run every 2 hours or every 4 hours?
2. **Search Queries**: Are you happy with our starting search keywords (`dengue`, `malaria`, `cholera` + cities)?
3. **Android Map Integration**: For the regional datamap, do you want us to plot custom map markers or a visual density heatmap on Google Maps in your Kotlin app?
