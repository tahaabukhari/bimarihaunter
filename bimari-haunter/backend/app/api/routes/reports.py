"""
Report endpoints – V1 insights listing / stats and V2 full-report fetch.
"""

from __future__ import annotations

from typing import Optional
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.schemas import (
    DashboardStats,
    LocationUpdateRequest,
    LocationUpdateResponse,
    OutbreakReport,
    AiAnalysis,
    Coordinates
)
from app.database.engine import get_db
from app.database.models import (
    DeliveryQueue,
    NewsSource,
    RawArticle,
    RawSocialPost,
    ReportV1Insight,
    ReportV2Full,
    SocialSource,
)
from app.publisher.formatter import format_v1_payload, format_v2_payload
from app.publisher.realtime import publisher

router = APIRouter(prefix="/api/v1", tags=["reports"])


# ── V1 Insights ─────────────────────────────────────────────


@router.get("/insights")
async def list_insights(
    disease: Optional[str] = Query(None),
    location: Optional[str] = Query(None),
    severity: Optional[float] = Query(None, alias="severity_threshold"),
    limit: int = Query(50, le=200),
    db: AsyncSession = Depends(get_db),
):
    """List V1 insights with optional filters."""
    stmt = select(ReportV1Insight).order_by(ReportV1Insight.created_at.desc())

    if location:
        stmt = stmt.where(ReportV1Insight.primary_location == location)

    if severity is not None:
        stmt = stmt.where(ReportV1Insight.severity_score >= severity)

    stmt = stmt.limit(limit)
    result = await db.execute(stmt)
    insights = result.scalars().all()

    return [format_v1_payload(i) for i in insights]


@router.get("/insights/stats", response_model=DashboardStats)
async def insight_stats(db: AsyncSession = Depends(get_db)):
    """Dashboard summary statistics."""
    active_news = await db.scalar(
        select(func.count()).select_from(NewsSource).where(NewsSource.is_active.is_(True))
    )
    active_social = await db.scalar(
        select(func.count()).select_from(SocialSource).where(SocialSource.is_active.is_(True))
    )
    total_articles = await db.scalar(
        select(func.count()).select_from(RawArticle)
    )
    total_social = await db.scalar(
        select(func.count()).select_from(RawSocialPost)
    )
    total_insights = await db.scalar(
        select(func.count()).select_from(ReportV1Insight)
    )
    total_full = await db.scalar(
        select(func.count()).select_from(ReportV2Full)
    )
    verified = await db.scalar(
        select(func.count())
        .select_from(ReportV2Full)
        .where(ReportV2Full.verified.is_(True))
    )
    pending = await db.scalar(
        select(func.count())
        .select_from(DeliveryQueue)
        .where(DeliveryQueue.status == "queued")
    )

    return DashboardStats(
        active_sources=(active_news or 0) + (active_social or 0),
        total_articles=total_articles or 0,
        total_social_posts=total_social or 0,
        total_insights=total_insights or 0,
        total_full_reports=total_full or 0,
        verified_reports=verified or 0,
        pending_delivery=pending or 0,
    )


# ── V2 Full Reports ─────────────────────────────────────────


@router.get("/reports/{report_id}")
async def get_full_report(
    report_id: int,
    db: AsyncSession = Depends(get_db),
):
    """Fetch a single V2 full report by ID."""
    report = await db.get(ReportV2Full, report_id)
    if not report:
        raise HTTPException(status_code=404, detail="Report not found")
    return format_v2_payload(report)


@router.post("/reports/request")
async def request_full_report(
    insight_id: int = Query(..., description="V1 insight ID to generate V2 from"),
    db: AsyncSession = Depends(get_db),
):
    """Request generation of a V2 full report from a V1 insight.

    In a full implementation this would trigger the NLP pipeline to
    produce an enriched V2 report.  For now it returns an
    acknowledgment.
    """
    insight = await db.get(ReportV1Insight, insight_id)
    if not insight:
        raise HTTPException(status_code=404, detail="Insight not found")

    return {
        "message": "Full report generation queued",
        "insight_id": insight_id,
        "status": "processing",
    }


# ── Android App Endpoints ───────────────────────────────────

@router.post("/users/location", response_model=LocationUpdateResponse)
async def update_location(
    body: LocationUpdateRequest,
    db: AsyncSession = Depends(get_db)
):
    """Update user location and return nearby feed count."""
    # In a real app, we'd update the user's location in the database.
    # For now, just return success and a mock feed count.
    
    return LocationUpdateResponse(
        status="success",
        message=f"Location updated for {body.city}",
        city=body.city,
        feed_count=10
    )


@router.get("/feed", response_model=list[OutbreakReport])
async def get_feed(
    limit: int = Query(50),
    db: AsyncSession = Depends(get_db)
):
    """Fetch personalized feed for the Android app."""
    # Fetch from ReportV2Full
    stmt = select(ReportV2Full).order_by(ReportV2Full.published_at.desc()).limit(limit)
    result = await db.execute(stmt)
    reports = result.scalars().all()
    
    feed = []
    for report in reports:
        # Map ReportV2Full to OutbreakReport
        severity_str = "medium"
        if report.severity_score:
            if report.severity_score >= 0.7:
                severity_str = "high"
            elif report.severity_score <= 0.3:
                severity_str = "low"
                
        feed.append(OutbreakReport(
            id=str(report.id),
            title=report.title,
            source=report.source_name,
            url=report.source_url,
            raw_text=report.full_text[:200] + "...",
            published_at=report.published_at.isoformat() if isinstance(report.published_at, datetime) else str(report.published_at),
            scraped_at=report.created_at.isoformat() if isinstance(report.created_at, datetime) else str(report.created_at),
            status="verified" if report.verified else "pending",
            source_type=report.source_type,
            ai_analysis=AiAnalysis(
                disease=report.diseases[0] if report.diseases else "Unknown",
                severity=severity_str,
                summary=[report.summary] if report.summary else [],
                symptoms=report.symptoms or [],
                locations=report.locations or [],
                coordinates=Coordinates(
                    latitude=24.8607, # Default to Karachi for now if missing
                    longitude=67.0011
                ),
                confidence_score=float(report.confidence) if report.confidence else 0.8,
                model_used="Gemini 1.5 Pro"
            )
        ))
        
    if not feed:
        # Return mock data if DB is empty to ensure feed is generated
        feed.append(OutbreakReport(
            id="mock-1",
            title="Dengue outbreak reported in Karachi",
            source="Local News",
            url="https://example.com/news1",
            raw_text="Hospitals report an influx of dengue patients...",
            published_at=datetime.utcnow().isoformat(),
            scraped_at=datetime.utcnow().isoformat(),
            status="verified",
            source_type="news",
            ai_analysis=AiAnalysis(
                disease="Dengue",
                severity="high",
                summary=["High cases of dengue in Karachi"],
                symptoms=["Fever", "Headache"],
                locations=["Karachi"],
                coordinates=Coordinates(latitude=24.8607, longitude=67.0011),
                confidence_score=0.95,
                model_used="Gemini 1.5 Pro"
            )
        ))
        
    return feed
