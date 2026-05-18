"""
Payload formatter for V1 (Insight) and V2 (Full Report) payloads.

Every payload pushed to an Android client via WebSocket must conform
exactly to the structures defined here.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any

from app.database.models import ReportV1Insight, ReportV2Full


def format_v1_payload(insight: ReportV1Insight) -> dict[str, Any]:
    """Build the V1 insight JSON payload for Android clients."""
    severity = float(insight.severity_score)
    return {
        "version": "v1",
        "type": "insight",
        "id": insight.id,
        "headline": insight.headline,
        "summary": insight.summary,
        "key_points": insight.key_points,
        "category": insight.category,
        "severity": severity,
        "urgency": insight.urgency_level,
        "location": {
            "primary": insight.primary_location,
            "geo": insight.geo_json,
        },
        "timestamp": (
            insight.published_at.isoformat()
            if isinstance(insight.published_at, datetime)
            else str(insight.published_at)
        ),
        "expires_at": (
            insight.expires_at.isoformat()
            if insight.expires_at and isinstance(insight.expires_at, datetime)
            else None
        ),
        "action_required": severity > 0.7,
        "tap_for_full_report": True,
    }


def format_v2_payload(report: ReportV2Full) -> dict[str, Any]:
    """Build the V2 full-report JSON payload for Android clients."""
    return {
        "version": "v2",
        "type": "full_report",
        "id": report.id,
        "title": report.title,
        "full_text": report.full_text,
        "summary": report.summary,
        "source": {
            "name": report.source_name,
            "url": report.source_url,
            "logo": report.source_logo_url,
        },
        "author": report.author_name,
        "media": report.media_urls or [],
        "entities": {
            "diseases": report.diseases or [],
            "symptoms": report.symptoms or [],
            "locations": report.locations or [],
            "affected": report.affected_count,
            "deaths": report.death_count,
            "recovered": report.recovered_count,
        },
        "classification": {
            "category": report.category,
            "severity": float(report.severity_score) if report.severity_score else None,
            "confidence": float(report.confidence) if report.confidence else None,
        },
        "geo": report.geo_json,
        "verified": report.verified,
        "published_at": (
            report.published_at.isoformat()
            if isinstance(report.published_at, datetime)
            else str(report.published_at)
        ),
        "scraped_at": (
            report.created_at.isoformat()
            if isinstance(report.created_at, datetime)
            else str(report.created_at)
        ),
    }
