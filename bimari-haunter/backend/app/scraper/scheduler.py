"""
Scrape scheduler – orchestrates web and social source scraping.

Creates ScrapeJob records, delegates to NewsCrawler / FacebookScraper,
deduplicates via SHA-256 content hashes, and triggers NLP processing.
"""

from __future__ import annotations

import asyncio
import hashlib
from datetime import datetime, timezone
from typing import Any, Optional, Sequence

import structlog
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.engine import async_session
from app.database.models import (
    NewsSource,
    RawArticle,
    RawSocialPost,
    ScrapeJob,
    SocialSource,
)
from app.nlp.processor import NLPProcessor
from app.scraper.crawler import NewsCrawler
from app.scraper.facebook_client import FacebookScraper
from app.nlp.classifier import classify_article
from app.services.firestore import get_firestore_client
import uuid

logger = structlog.get_logger(__name__)


class ScrapeScheduler:
    """Coordinates concurrent scraping of all active sources."""

    def __init__(self, *, max_concurrent: int = 3) -> None:
        self.max_concurrent = max_concurrent
        self._semaphore = asyncio.Semaphore(max_concurrent)
        self.nlp = NLPProcessor()

    async def run_all(
        self,
        *,
        source_ids: Optional[Sequence[int]] = None,
    ) -> None:
        """Scrape every active source (optionally filtered by IDs)."""
        async with async_session() as db:
            # Fetch news sources
            stmt = select(NewsSource).where(NewsSource.is_active.is_(True))
            if source_ids:
                stmt = stmt.where(NewsSource.id.in_(source_ids))
            news_sources = (await db.execute(stmt)).scalars().all()

            # Fetch social sources
            stmt = select(SocialSource).where(SocialSource.is_active.is_(True))
            if source_ids:
                stmt = stmt.where(SocialSource.id.in_(source_ids))
            social_sources = (await db.execute(stmt)).scalars().all()

        tasks: list[asyncio.Task] = []
        for src in news_sources:
            tasks.append(asyncio.create_task(self._scrape_news(src)))
        for src in social_sources:
            tasks.append(asyncio.create_task(self._scrape_social(src)))

        results = await asyncio.gather(*tasks, return_exceptions=True)
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.error("scrape_task_failed", index=i, error=str(result))

    # ── News scraping ───────────────────────────────────────

    async def _scrape_news(self, source: NewsSource) -> None:
        async with self._semaphore:
            async with async_session() as db:
                job = ScrapeJob(
                    source_id=source.id,
                    source_type="web",
                    job_type="scrape",
                    status="running",
                    started_at=datetime.now(timezone.utc),
                )
                db.add(job)
                await db.commit()
                await db.refresh(job)

                try:
                    crawler = NewsCrawler(source.scrape_config)
                    urls = await crawler.discover_articles()
                    items_found = len(urls)
                    items_stored = 0

                    for url in urls:
                        article_data = await crawler.fetch_article(url)
                        text = article_data.get("text") or ""
                        raw_html = article_data.get("raw_html") or ""

                        content = text or raw_html
                        if not content:
                            continue

                        content_hash = hashlib.sha256(content.encode()).hexdigest()

                        # Upsert – skip duplicates
                        stmt = (
                            pg_insert(RawArticle)
                            .values(
                                source_id=source.id,
                                job_id=job.id,
                                url=url,
                                title=article_data.get("title"),
                                raw_html=raw_html,
                                extracted_text=text,
                                published_at=article_data.get("published_at"),
                                content_hash=content_hash,
                                processing_status="pending",
                            )
                            .on_conflict_do_nothing(index_elements=["content_hash"])
                        )
                        result = await db.execute(stmt)
                        if result.rowcount:
                            items_stored += 1
                            try:
                                classification = classify_article(text)
                                fs = get_firestore_client()
                                article_id = str(uuid.uuid4())
                                
                                region = "Punjab"
                                for city in ["Lahore", "Karachi", "Islamabad", "Rawalpindi", "Peshawar", "Quetta", "Multan", "Faisalabad"]:
                                    if city.lower() in text.lower() or city.lower() in (article_data.get("title") or "").lower():
                                        region = city
                                        break
                                        
                                doc_data = {
                                    "id": article_id,
                                    "title": article_data.get("title") or "Untitled",
                                    "body": text,
                                    "source": "news",
                                    "category": classification["category"],
                                    "confidence": classification["confidence"],
                                    "region": region,
                                    "timestamp": datetime.now(timezone.utc).isoformat(),
                                    "url": url
                                }
                                fs.collection("articles").document(article_id).set(doc_data)
                                logger.info("article_stored_in_firestore", id=article_id, category=classification["category"])
                            except Exception as fs_exc:
                                logger.error("firestore_article_storage_failed", url=url, error=str(fs_exc))

                    job.status = "completed"
                    job.items_found = items_found
                    job.items_stored = items_stored
                    job.completed_at = datetime.now(timezone.utc)
                    source.last_scraped_at = datetime.now(timezone.utc)
                    await db.commit()

                    logger.info(
                        "news_scrape_complete",
                        source=source.name,
                        found=items_found,
                        stored=items_stored,
                    )

                except Exception as exc:
                    job.status = "failed"
                    job.error_message = str(exc)[:500]
                    job.completed_at = datetime.now(timezone.utc)
                    await db.commit()
                    logger.error(
                        "news_scrape_failed",
                        source=source.name,
                        error=str(exc),
                    )

    # ── Social scraping ─────────────────────────────────────

    async def _scrape_social(self, source: SocialSource) -> None:
        async with self._semaphore:
            async with async_session() as db:
                job = ScrapeJob(
                    source_id=source.id,
                    source_type="social",
                    job_type="scrape",
                    status="running",
                    started_at=datetime.now(timezone.utc),
                )
                db.add(job)
                await db.commit()
                await db.refresh(job)

                try:
                    scraper = FacebookScraper(source.api_config)
                    posts = await scraper.scrape(source.external_id)
                    items_found = len(posts)
                    items_stored = 0

                    for post in posts:
                        stmt = (
                            pg_insert(RawSocialPost)
                            .values(
                                source_id=source.id,
                                job_id=job.id,
                                platform=source.platform,
                                external_post_id=post["external_post_id"],
                                permalink=post.get("permalink"),
                                raw_text=post.get("raw_text"),
                                cleaned_text=post.get("cleaned_text"),
                                media_urls=post.get("media_urls"),
                                published_at=post.get("published_at"),
                                likes_count=post.get("likes_count", 0),
                                comments_count=post.get("comments_count", 0),
                                shares_count=post.get("shares_count", 0),
                                content_hash=post["content_hash"],
                                processing_status="pending",
                            )
                            .on_conflict_do_nothing(
                                index_elements=["content_hash"]
                            )
                        )
                        result = await db.execute(stmt)
                        if result.rowcount:
                            items_stored += 1
                            try:
                                text = post.get("raw_text") or post.get("cleaned_text") or ""
                                if text:
                                    classification = classify_article(text)
                                    fs = get_firestore_client()
                                    post_id = str(uuid.uuid4())
                                    
                                    region = "Punjab"
                                    for city in ["Lahore", "Karachi", "Islamabad", "Rawalpindi", "Peshawar", "Quetta", "Multan", "Faisalabad"]:
                                        if city.lower() in text.lower():
                                            region = city
                                            break
                                            
                                    platform = source.platform.lower() if source.platform else "facebook"
                                    if platform not in ["facebook", "twitter"]:
                                        platform = "facebook"
                                        
                                    doc_data = {
                                        "id": post_id,
                                        "title": text[:60] + "..." if len(text) > 60 else text,
                                        "body": text,
                                        "source": platform,
                                        "category": classification["category"],
                                        "confidence": classification["confidence"],
                                        "region": region,
                                        "timestamp": datetime.now(timezone.utc).isoformat(),
                                        "url": post.get("permalink") or ""
                                    }
                                    fs.collection("articles").document(post_id).set(doc_data)
                                    logger.info("social_post_stored_in_firestore", id=post_id, category=classification["category"])
                            except Exception as fs_exc:
                                logger.error("firestore_social_storage_failed", post_id=post.get("external_post_id"), error=str(fs_exc))

                    job.status = "completed"
                    job.items_found = items_found
                    job.items_stored = items_stored
                    job.completed_at = datetime.now(timezone.utc)
                    source.last_fetched_at = datetime.now(timezone.utc)
                    await db.commit()

                    logger.info(
                        "social_scrape_complete",
                        source=source.name,
                        found=items_found,
                        stored=items_stored,
                    )

                except Exception as exc:
                    job.status = "failed"
                    job.error_message = str(exc)[:500]
                    job.completed_at = datetime.now(timezone.utc)
                    await db.commit()
                    logger.error(
                        "social_scrape_failed",
                        source=source.name,
                        error=str(exc),
                    )
