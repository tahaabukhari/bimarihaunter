"""
Facebook Graph API v18.0 client and health-post scraper.

All Facebook data is fetched exclusively through the official Graph
API – web scraping of Facebook is never performed.
"""

from __future__ import annotations

import asyncio
import hashlib
import re
import time
from datetime import datetime, timedelta, timezone
from typing import Any, Optional

import httpx
import structlog

logger = structlog.get_logger(__name__)

# ── Graph API client ───────────────────────────────────────


class FacebookGraphClient:
    """Low-level async wrapper around the Facebook Graph API v18.0."""

    BASE_URL: str = "https://graph.facebook.com/v18.0"

    def __init__(
        self,
        access_token: str,
        *,
        max_concurrent: int = 5,
        rate_limit_per_hour: int = 200,
    ) -> None:
        self.access_token = access_token
        self._semaphore = asyncio.Semaphore(max_concurrent)
        self._rate_limit = rate_limit_per_hour
        self._request_count = 0
        self._window_start = time.monotonic()

    async def _request(
        self,
        endpoint: str,
        params: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        """Make a rate-limited GET request to the Graph API."""
        await self._enforce_rate_limit()

        url = f"{self.BASE_URL}/{endpoint}"
        params = params or {}
        params["access_token"] = self.access_token

        async with self._semaphore:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.get(url, params=params)
                resp.raise_for_status()
                self._request_count += 1
                return resp.json()

    async def _enforce_rate_limit(self) -> None:
        """Sleep if the hourly request budget is exhausted."""
        elapsed = time.monotonic() - self._window_start
        if elapsed >= 3600:
            self._request_count = 0
            self._window_start = time.monotonic()
            return
        if self._request_count >= self._rate_limit:
            sleep_for = 3600 - elapsed
            logger.warning("rate_limit_reached", sleep_seconds=sleep_for)
            await asyncio.sleep(sleep_for)
            self._request_count = 0
            self._window_start = time.monotonic()

    # ── Public endpoints ────────────────────────────────────

    async def get_page_posts(
        self,
        page_id: str,
        *,
        limit: int = 25,
        since: Optional[str] = None,
    ) -> list[dict[str, Any]]:
        """Fetch posts from a Facebook Page."""
        params: dict[str, Any] = {
            "fields": (
                "id,message,created_time,permalink_url,"
                "attachments,likes.summary(true),"
                "comments.summary(true),shares"
            ),
            "limit": limit,
        }
        if since:
            params["since"] = since

        data = await self._request(f"{page_id}/posts", params)
        return data.get("data", [])

    async def get_post_comments(
        self,
        post_id: str,
        *,
        limit: int = 25,
    ) -> list[dict[str, Any]]:
        """Fetch comments on a specific post."""
        params: dict[str, Any] = {
            "fields": "id,message,created_time,from,likes.summary(true)",
            "limit": limit,
        }
        data = await self._request(f"{post_id}/comments", params)
        return data.get("data", [])

    async def search_public_posts(
        self,
        query: str,
        *,
        limit: int = 25,
    ) -> list[dict[str, Any]]:
        """Search public posts (requires special Page Public Content Access).

        Note: This permission is restricted and may not be available.
        """
        params: dict[str, Any] = {
            "q": query,
            "type": "post",
            "limit": limit,
        }
        try:
            data = await self._request("search", params)
            return data.get("data", [])
        except httpx.HTTPStatusError as exc:
            logger.warning(
                "public_post_search_unavailable",
                status=exc.response.status_code,
            )
            return []


# ── Health-focused scraper ─────────────────────────────────

DISEASE_KEYWORDS_URDU: list[str] = [
    "ڈینگی", "ملیریا", "کووڈ", "وائرس", "بخار",
    "وبا", "ہسپتال", "مریض", "ویکسین", "علاج",
]

DISEASE_KEYWORDS_EN: list[str] = [
    "dengue", "malaria", "covid", "virus", "fever",
    "outbreak", "hospital", "patient", "vaccine",
    "treatment", "disease",
]


class FacebookScraper:
    """High-level scraper that uses FacebookGraphClient to fetch
    health-related posts from configured Facebook Pages."""

    def __init__(self, api_config: dict[str, Any]) -> None:
        access_token = api_config.get("access_token", "")
        rate_limit = api_config.get("rate_limit", 200)
        self.client = FacebookGraphClient(
            access_token=access_token,
            rate_limit_per_hour=rate_limit,
        )

    async def scrape(
        self,
        page_id: str,
        *,
        days_back: int = 7,
    ) -> list[dict[str, Any]]:
        """Fetch and filter health-related posts from *page_id*."""
        since_dt = datetime.now(timezone.utc) - timedelta(days=days_back)
        since_str = since_dt.strftime("%Y-%m-%dT%H:%M:%S")

        raw_posts = await self.client.get_page_posts(
            page_id, limit=50, since=since_str
        )

        results: list[dict[str, Any]] = []
        for post in raw_posts:
            message = post.get("message", "")
            if not self.is_health_related(message):
                continue

            content_hash = hashlib.sha256(
                (post["id"] + message[:200]).encode()
            ).hexdigest()

            media_urls = self._extract_media(post)
            likes = (
                post.get("likes", {}).get("summary", {}).get("total_count", 0)
            )
            comments = (
                post.get("comments", {}).get("summary", {}).get("total_count", 0)
            )
            shares = post.get("shares", {}).get("count", 0)

            cleaned = self._clean_text(message)

            results.append(
                {
                    "external_post_id": post["id"],
                    "permalink": post.get("permalink_url"),
                    "raw_text": message,
                    "cleaned_text": cleaned,
                    "media_urls": media_urls,
                    "published_at": post.get("created_time"),
                    "likes_count": likes,
                    "comments_count": comments,
                    "shares_count": shares,
                    "content_hash": content_hash,
                }
            )

        logger.info(
            "facebook_scrape_complete",
            page_id=page_id,
            total=len(raw_posts),
            health_related=len(results),
        )
        return results

    # ── Helpers ─────────────────────────────────────────────

    @staticmethod
    def is_health_related(text: str) -> bool:
        """Return True if *text* contains any health/disease keyword."""
        lower = text.lower()
        for kw in DISEASE_KEYWORDS_EN:
            if kw in lower:
                return True
        for kw in DISEASE_KEYWORDS_URDU:
            if kw in text:
                return True
        return False

    @staticmethod
    def _clean_text(text: str) -> str:
        """Remove URLs and collapse whitespace."""
        text = re.sub(r"https?://\S+", "", text)
        text = re.sub(r"\s+", " ", text)
        return text.strip()

    @staticmethod
    def _extract_media(post: dict[str, Any]) -> list[str]:
        """Pull photo / video URLs from the attachments subgraph."""
        urls: list[str] = []
        attachments = post.get("attachments", {}).get("data", [])
        for att in attachments:
            att_type = att.get("type", "")
            if att_type in ("photo", "video", "video_autoplay"):
                media = att.get("media", {})
                image = media.get("image", {})
                src = image.get("src") or media.get("source")
                if src:
                    urls.append(src)
            sub_attachments = att.get("subattachments", {}).get("data", [])
            for sub in sub_attachments:
                sub_media = sub.get("media", {})
                sub_image = sub_media.get("image", {})
                sub_src = sub_image.get("src")
                if sub_src:
                    urls.append(sub_src)
        return urls
