"""
Lean Playwright-based scraper optimized for Cloud Run.

Features:
- Reads users from Firestore `/users`
- For each user, scrapes X (Twitter) search results for their city+diseases
- Filters posts by disease keywords, deduplicates (SHA-256)
- Writes relevant reports to `/reports` and updates user's `last_scrape_timestamp`

Run locally:
    python bimarihaunter_scraper_optimized.py

Run scheduler:
    python bimarihaunter_scraper_optimized.py --schedule
"""

from __future__ import annotations

import asyncio
import hashlib
import json
import os
from datetime import datetime, timezone
from typing import List

from playwright.async_api import async_playwright, TimeoutError as PlaywrightTimeout
from firebase_admin import firestore

from app.database.firestore import db


async def fetch_search_posts(page, query: str, max_posts: int = 25) -> List[dict]:
    url = f"https://twitter.com/search?q={query}&f=live"
    await page.goto(url, timeout=30000)
    await page.wait_for_selector("article", timeout=10000)

    articles = page.locator("article")
    count = min(await articles.count(), max_posts)
    results: List[dict] = []

    for i in range(count):
        art = articles.nth(i)
        try:
            raw_text = (await art.inner_text()) or ""
            url_el = art.locator('a[href*="/status/"]').first
            href = await url_el.get_attribute("href")
            if href and href.startswith("/"):
                full_url = f"https://twitter.com{href}"
            else:
                full_url = href or ""

            time_el = art.locator("time")
            published_at = None
            if await time_el.count() > 0:
                ts = await time_el.get_attribute("datetime")
                published_at = ts

            results.append({
                "text": raw_text.strip(),
                "url": full_url,
                "published_at": published_at,
            })
        except PlaywrightTimeout:
            continue
        except Exception:
            continue

    return results


def is_relevant(text: str, disease_keywords: List[str]) -> bool:
    t = text.lower()
    for kw in disease_keywords:
        if kw.lower() in t:
            return True
    return False


def make_hash(user_id: str, url: str, text: str) -> str:
    h = hashlib.sha256()
    h.update(user_id.encode("utf-8"))
    h.update(b"||")
    h.update((url or "").encode("utf-8"))
    h.update(b"||")
    h.update(text.encode("utf-8"))
    return h.hexdigest()


async def run_once(max_per_user: int = 25):
    users_ref = db.collection("users")
    users = list(users_ref.stream())

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context()
        page = await context.new_page()

        for u_doc in users:
            try:
                u = u_doc.to_dict()
                uid = u.get("uid") or u_doc.id
                prefs = u.get("preferences") or {}
                diseases = prefs.get("diseases", [])
                city = (u.get("city") or u.get("live_location", {}).get("city") or "").strip()
                if not diseases or not city:
                    continue

                # build disease keywords
                disease_keywords = [d.lower() for d in diseases]

                posted_hashes = set()

                for disease in diseases:
                    query = f"{disease} {city}"
                    # simple urlencoded query
                    q_enc = os.path.basename(query).replace(" ", "%20")
                    try:
                        posts = await fetch_search_posts(page, q_enc, max_posts=max_per_user)
                    except Exception:
                        posts = []

                    for p_item in posts:
                        text = p_item.get("text", "")
                        url = p_item.get("url", "")
                        if not text:
                            continue
                        if not is_relevant(text, disease_keywords):
                            continue

                        h = make_hash(uid, url, text)
                        if h in posted_hashes:
                            continue

                        posted_hashes.add(h)

                        report = {
                            "user_id": uid,
                            "disease": disease,
                            "title": (text[:140] + "...") if len(text) > 140 else text,
                            "raw_text": text,
                            "url": url,
                            "published_at": p_item.get("published_at"),
                            "scraped_at": datetime.now(timezone.utc).isoformat(),
                            "dedupe_hash": h,
                            "source": "x",
                        }

                        # use hash as id to make reports idempotent per user
                        doc_id = h
                        try:
                            db.collection("reports").document(doc_id).set(report, merge=True)
                        except Exception:
                            continue

                # update user's last_scrape_timestamp
                try:
                    users_ref.document(uid).update({
                        "last_scrape_timestamp": datetime.now(timezone.utc).isoformat(),
                        "scrape_status": "ok"
                    })
                except Exception:
                    pass

        await context.close()
        await browser.close()


async def scheduler_loop(interval_seconds: int = 60 * 60 * 4):
    while True:
        await run_once()
        await asyncio.sleep(interval_seconds)


def main():
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--schedule", action="store_true", help="Run as recurring scheduler every 4 hours")
    args = parser.parse_args()

    if args.schedule:
        asyncio.run(scheduler_loop())
    else:
        asyncio.run(run_once())


if __name__ == "__main__":
    main()
