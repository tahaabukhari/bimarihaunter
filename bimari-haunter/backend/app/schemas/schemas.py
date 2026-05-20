"""
Pydantic request / response schemas for the BimariHaunter API.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, List, Optional
from pydantic import BaseModel, HttpUrl

# ── Android App Support Schemas ────────────────────────────

class LocationUpdateRequest(BaseModel):
    city: str
    latitude: float
    longitude: float

class LocationUpdateResponse(BaseModel):
    status: str
    message: str
    city: str
    feed_count: int

class Coordinates(BaseModel):
    latitude: float
    longitude: float

class AiAnalysis(BaseModel):
    disease: str
    severity: str
    summary: list[str]
    symptoms: list[str]
    locations: list[str]
    coordinates: Coordinates
    confidence_score: float
    model_used: str

class OutbreakReport(BaseModel):
    id: str
    title: str
    source: str
    url: str
    raw_text: str
    published_at: str
    scraped_at: str
    status: str
    source_type: str
    ai_analysis: AiAnalysis


class UserRegisterRequest(BaseModel):
    uid: str
    email: str
    name: str
    phone_number: Optional[str] = None
    avatar_url: Optional[str] = None


class UserPreferencesRequest(BaseModel):
    diseases: List[str]
    feed_tags: Optional[List[str]] = None   # Full list of user-selected tag IDs
    city: str
    latitude: float
    longitude: float
    radius: int = 50
