"""
Scrape scheduler – orchestrates web and social source scraping.

RESERVE VERSION with Playwright Social Media Scrapers integrated.
"""

from __future__ import annotations

import asyncio
import hashlib
from datetime import datetime, timezone
from typing import Any, Optional, Sequence

import structlog

# Imports for Firestore and AI pipeline
from app.database.firestore import db
from google.cloud import firestore
from app.nlp.processor import NLPProcessor
from app.scraper.crawler import NewsCrawler
from reserve.social_playwright import PlaywrightSocialScraper

logger = structlog.get_logger(__name__)

# Geo-coordinate resolution mapping for major cities/regions in Pakistan
COORDINATES_MAP = {
    "karachi": (24.8607, 67.0011),
    "lahore": (31.5204, 74.3587),
    "islamabad": (33.6844, 73.0479),
    "rawalpindi": (33.5984, 73.0441),
    "peshawar": (34.0151, 71.5249),
    "quetta": (30.1798, 66.9750),
    "multan": (30.1575, 71.5249),
    "faisalabad": (31.4504, 73.1350),
    "sialkot": (32.4925, 74.5310),
    "gujranwala": (32.1877, 74.1945),
    "sindh": (25.8920, 68.5247),
    "punjab": (31.1704, 72.7097),
    "kpk": (34.9526, 72.3311),
    "balochistan": (28.4907, 65.0958),
}

# Targeted public health social search queries
SOCIAL_SEARCH_QUERIES = [
    "dengue Karachi",
    "dengue Lahore",
    "malaria Punjab",
    "cholera Sindh",
    "outbreak Peshawar",
    "dengue Rawalpindi",
]

class ScrapeScheduler:
    """Coordinates concurrent scraping and fires results to Firestore."""

    def __init__(self, *, max_concurrent: int = 3) -> None:
        self.max_concurrent = max_concurrent
        self._semaphore = asyncio.Semaphore(max_concurrent)
        self.nlp = NLPProcessor()
        self.social_scraper = PlaywrightSocialScraper(use_headless=True)

    async def run_all(
        self,
        *,
        source_ids: Optional[Sequence[str]] = None,
    ) -> None:
        """Scrape news articles and social media updates from Firestore."""
        
        # 1. Fetch news sources from Firestore
        news_ref = db.collection("news_sources")
        news_docs = news_ref.where("is_active", "==", True).stream()
        news_sources = []
        for doc in news_docs:
            data = doc.to_dict()
            data["id"] = doc.id
            news_sources.append(data)
            
        # 2. If news_sources is completely empty, seed it with default pakistani sources
        if not news_sources:
            from app.scraper.sources.pakistani_news import PAKISTANI_NEWS_SOURCES
            logger.info("seeding_firestore_news_sources")
            for idx, source in enumerate(PAKISTANI_NEWS_SOURCES):
                source_id = str(idx + 1)
                doc_ref = news_ref.document(source_id)
                doc_ref.set({
                    "id": source_id,
                    "name": source["name"],
                    "base_url": source["base_url"],
                    "region": source["region"],
                    "country": source["country"],
                    "language": source["language"],
                    "scrape_config": source["scrape_config"],
                    "schedule": source["schedule"],
                    "is_active": True
                })
                source["id"] = source_id
                source["is_active"] = True
                news_sources.append(source)

        # Filter news by source_ids if provided
        if source_ids:
            news_sources = [s for s in news_sources if s["id"] in source_ids]

        tasks: list[asyncio.Task] = []
        
        # Add news tasks
        for src in news_sources:
            tasks.append(asyncio.create_task(self._scrape_news(src)))
            
        # Add Playwright social media search tasks
        for query in SOCIAL_SEARCH_QUERIES:
            tasks.append(asyncio.create_task(self._scrape_social_playwright(query)))

        results = await asyncio.gather(*tasks, return_exceptions=True)
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.error("scrape_task_failed", index=i, error=str(result))

    # ── News scraping & Firestore Upload ────────────────────

    async def _scrape_news(self, source: dict) -> None:
        async with self._semaphore:
            job_ref = db.collection("scrape_jobs").document()
            job_id = job_ref.id
            
            job_ref.set({
                "job_id": job_id,
                "source_id": source["id"],
                "source_name": source["name"],
                "source_type": "web",
                "job_type": "scrape",
                "status": "running",
                "started_at": datetime.now(timezone.utc),
                "items_found": 0,
                "items_stored": 0
            })

            try:
                crawler = NewsCrawler(source["scrape_config"])
                urls = await crawler.discover_articles()
                items_found = len(urls)
                items_stored = 0

                for url in urls:
                    url_hash = hashlib.sha256(url.encode()).hexdigest()
                    doc_ref = db.collection("reports").document(url_hash)
                    doc_snap = doc_ref.get()

                    if doc_snap.exists:
                        logger.debug("duplicate_prevented", url=url, hash=url_hash)
                        continue

                    article_data = await crawler.fetch_article(url)
                    text = article_data.get("text") or ""
                    raw_html = article_data.get("raw_html") or ""

                    content = text or raw_html
                    if not content:
                        continue

                    published_at = article_data.get("published_at")
                    if not published_at:
                        published_at = datetime.now(timezone.utc)

                    raw_doc = {
                        "title": article_data.get("title") or "Outbreak Update",
                        "source": source["name"],
                        "url": url,
                        "raw_text": content,
                        "published_at": published_at,
                        "scraped_at": datetime.now(timezone.utc),
                        "status": "scraped",
                        "source_type": "web",
                        "ai_analysis": {}
                    }
                    doc_ref.set(raw_doc)

                    try:
                        nlp_result = self.nlp.process(content)

                        sev_score = nlp_result.get("severity", 0.0)
                        severity_str = "high" if sev_score >= 0.6 else "medium" if sev_score >= 0.3 else "low"

                        summary_str = nlp_result.get("summary", "")
                        summary_array = [s.strip() for s in summary_str.split(".") if s.strip()]

                        diseases = nlp_result["entities"].get("diseases", [])
                        detected_disease = diseases[0].lower() if diseases else "general outbreak"

                        locations = nlp_result["entities"].get("locations", [])
                        lat, lon = 30.3753, 69.3451
                        for loc in locations:
                            loc_lower = loc.lower().strip()
                            if loc_lower in COORDINATES_MAP:
                                lat, lon = COORDINATES_MAP[loc_lower]
                                break

                        ai_analysis = {
                            "disease": detected_disease,
                            "severity": severity_str,
                            "summary": summary_array,
                            "symptoms": nlp_result["entities"].get("symptoms", []),
                            "locations": locations,
                            "coordinates": firestore.GeoPoint(lat, lon),
                            "confidence_score": nlp_result["classification"].get("score", 1.0),
                            "model_used": nlp_result["model_metadata"].get("classifier", "facebook/bart-large-mnli")
                        }

                        doc_ref.update({
                            "ai_analysis": ai_analysis,
                            "status": "analyzed"
                        })
                        items_stored += 1

                    except Exception as nlp_err:
                        logger.error("nlp_enrichment_failed", url=url, error=str(nlp_err))
                        doc_ref.update({
                            "status": "failed"
                        })

                job_ref.update({
                    "status": "completed",
                    "items_found": items_found,
                    "items_stored": items_stored,
                    "completed_at": datetime.now(timezone.utc)
                })

                db.collection("news_sources").document(source["id"]).update({
                    "last_scraped_at": datetime.now(timezone.utc)
                })

                logger.info(
                    "news_scrape_complete",
                    source=source["name"],
                    found=items_found,
                    stored=items_stored,
                )

            except Exception as exc:
                job_ref.update({
                    "status": "failed",
                    "error_message": str(exc)[:500],
                    "completed_at": datetime.now(timezone.utc)
                })
                logger.error(
                    "news_scrape_failed",
                    source=source["name"],
                    error=str(exc),
                )

    # ── Playwright Social Scraping & Firestore Upload ───────

    async def _scrape_social_playwright(self, query: str) -> None:
        """Runs keyword searches concurrently on X and Facebook via Playwright."""
        async with self._semaphore:
            job_ref = db.collection("scrape_jobs").document()
            job_id = job_ref.id
            
            job_ref.set({
                "job_id": job_id,
                "source_name": f"Social Search: {query}",
                "source_type": "social",
                "job_type": "scrape",
                "status": "running",
                "started_at": datetime.now(timezone.utc),
                "items_found": 0,
                "items_stored": 0
            })

            try:
                # Scrape X and Facebook concurrently using Playwright
                x_task = self.social_scraper.scrape_x(query, limit=10)
                fb_task = self.social_scraper.scrape_facebook(query, limit=10)
                
                x_posts, fb_posts = await asyncio.gather(x_task, fb_task)
                all_posts = x_posts + fb_posts
                
                items_found = len(all_posts)
                items_stored = 0

                for post in all_posts:
                    content_hash = post["content_hash"]
                    doc_ref = db.collection("reports").document(content_hash)
                    doc_snap = doc_ref.get()

                    # Deduplication
                    if doc_snap.exists:
                        continue

                    # Write raw social document
                    raw_doc = {
                        "title": f"Social Update from {post['author']} ({post['platform'].upper()})",
                        "source": f"{post['platform'].capitalize()} Search",
                        "url": post["permalink"],
                        "raw_text": post["raw_text"],
                        "published_at": post["published_at"],
                        "scraped_at": datetime.now(timezone.utc),
                        "status": "scraped",
                        "source_type": "social",
                        "platform": post["platform"],
                        "ai_analysis": {}
                    }
                    doc_ref.set(raw_doc)

                    # Enrich using local NLP
                    try:
                        nlp_result = self.nlp.process(post["cleaned_text"])

                        sev_score = nlp_result.get("severity", 0.0)
                        severity_str = "high" if sev_score >= 0.6 else "medium" if sev_score >= 0.3 else "low"

                        summary_str = nlp_result.get("summary", "")
                        summary_array = [s.strip() for s in summary_str.split(".") if s.strip()]

                        diseases = nlp_result["entities"].get("diseases", [])
                        detected_disease = diseases[0].lower() if diseases else "general outbreak"

                        # Resolve coordinates
                        lat, lon = 30.3753, 69.3451
                        if post["coordinates"]:
                            lat, lon = post["coordinates"]
                        else:
                            locations = nlp_result["entities"].get("locations", [])
                            for loc in locations:
                                loc_lower = loc.lower().strip()
                                if loc_lower in COORDINATES_MAP:
                                    lat, lon = COORDINATES_MAP[loc_lower]
                                    break

                        ai_analysis = {
                            "disease": detected_disease,
                            "severity": severity_str,
                            "summary": summary_array,
                            "symptoms": nlp_result["entities"].get("symptoms", []),
                            "locations": post["locations"] or nlp_result["entities"].get("locations", []),
                            "coordinates": firestore.GeoPoint(lat, lon),
                            "confidence_score": nlp_result["classification"].get("score", 1.0),
                            "model_used": nlp_result["model_metadata"].get("classifier", "facebook/bart-large-mnli")
                        }

                        doc_ref.update({
                            "ai_analysis": ai_analysis,
                            "status": "analyzed"
                        })
                        items_stored += 1

                    except Exception as nlp_err:
                        logger.error("nlp_enrichment_failed_social_playwright", error=str(nlp_err))
                        doc_ref.update({
                            "status": "failed"
                        })

                # Log final job stats in Firestore
                job_ref.update({
                    "status": "completed",
                    "items_found": items_found,
                    "items_stored": items_stored,
                    "completed_at": datetime.now(timezone.utc)
                })

                logger.info(
                    "social_playwright_scrape_complete",
                    query=query,
                    found=items_found,
                    stored=items_stored,
                )

            except Exception as exc:
                job_ref.update({
                    "status": "failed",
                    "error_message": str(exc)[:500],
                    "completed_at": datetime.now(timezone.utc)
                })
                logger.error(
                    "social_playwright_scrape_failed",
                    query=query,
                    error=str(exc),
                )
