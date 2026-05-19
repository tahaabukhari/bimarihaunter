"""
Pydantic request / response schemas for the BimariHaunter API.
"""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Any, List, Literal, Optional

from pydantic import BaseModel, HttpUrl


# ── News sources ────────────────────────────────────────────


class SourceCreate(BaseModel):
    name: str
    base_url: HttpUrl
    region: Optional[str] = None
    country: str = "Pakistan"
    language: str = "mixed"
    scrape_config: dict[str, Any] = {}
    schedule: str = "0 */6 * * *"


class SourceResponse(SourceCreate):
    id: int
    is_active: bool
    last_scraped_at: Optional[datetime] = None
    created_at: datetime

    model_config = {"from_attributes": True}


# ── Social sources ──────────────────────────────────────────


class SocialSourceCreate(BaseModel):
    platform: str
    source_type: str
    external_id: str
    name: str
    url: Optional[str] = None
    api_config: dict[str, Any] = {}
    region: Optional[str] = None
    language: str = "mixed"


class SocialSourceResponse(SocialSourceCreate):
    id: int
    is_active: bool
    last_fetched_at: Optional[datetime] = None
    created_at: datetime

    model_config = {"from_attributes": True}


# ── Jobs ────────────────────────────────────────────────────


class JobResponse(BaseModel):
    id: int
    source_id: Optional[int] = None
    source_type: str
    job_type: str
    status: str
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    items_found: int = 0
    items_stored: int = 0
    error_message: Optional[str] = None
    created_at: datetime

    model_config = {"from_attributes": True}


# ── V1 Insight payload ─────────────────────────────────────


class InsightPayload(BaseModel):
    version: Literal["v1"] = "v1"
    type: Literal["insight"] = "insight"
    id: int
    headline: str
    summary: str
    key_points: list[Any]
    category: str
    severity: float
    urgency: Optional[str] = None
    location: dict[str, Any]
    timestamp: str
    expires_at: Optional[str] = None
    action_required: bool
    tap_for_full_report: bool = True


# ── V2 Full Report payload ─────────────────────────────────


class FullReportPayload(BaseModel):
    version: Literal["v2"] = "v2"
    type: Literal["full_report"] = "full_report"
    id: int
    title: str
    full_text: str
    summary: Optional[str] = None
    source: dict[str, Any]
    author: Optional[str] = None
    media: list[Any] = []
    entities: dict[str, Any]
    classification: dict[str, Any]
    geo: Optional[dict] = None
    verified: bool = False
    published_at: str
    scraped_at: str


# ── App sessions ────────────────────────────────────────────


class AppSessionCreate(BaseModel):
    device_id: str
    fcm_token: Optional[str] = None
    user_preferences: dict[str, Any] = {}


# ── Delivery status ────────────────────────────────────────


class DeliveryStatus(BaseModel):
    report_id: int
    version: str
    status: str
    delivered_at: Optional[datetime] = None
    read_at: Optional[datetime] = None


# ── Dashboard stats ─────────────────────────────────────────


class DashboardStats(BaseModel):
    active_sources: int
    total_articles: int
    total_social_posts: int
    total_insights: int
    total_full_reports: int
    verified_reports: int
    pending_delivery: int


# ── User location ───────────────────────────────────────────


class UserLocationUpdate(BaseModel):
    city: str
    latitude: float
    longitude: float

