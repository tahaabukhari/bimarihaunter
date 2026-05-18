"""
Source management endpoints.

GET / POST for both news_sources and social_sources.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.schemas import (
    SourceCreate,
    SourceResponse,
    SocialSourceCreate,
    SocialSourceResponse,
)
from app.database.engine import get_db
from app.database.models import NewsSource, SocialSource

router = APIRouter(prefix="/api/v1", tags=["sources"])


# ── News sources ────────────────────────────────────────────


@router.get("/sources", response_model=list[SourceResponse])
async def list_sources(db: AsyncSession = Depends(get_db)):
    """List all active news sources."""
    stmt = select(NewsSource).where(NewsSource.is_active.is_(True))
    result = await db.execute(stmt)
    return result.scalars().all()


@router.post("/sources", response_model=SourceResponse, status_code=201)
async def create_source(
    body: SourceCreate,
    db: AsyncSession = Depends(get_db),
):
    """Create a new news source."""
    source = NewsSource(
        name=body.name,
        base_url=str(body.base_url),
        region=body.region,
        country=body.country,
        language=body.language,
        scrape_config=body.scrape_config,
        schedule=body.schedule,
    )
    db.add(source)
    await db.flush()
    await db.refresh(source)
    return source


# ── Social sources ──────────────────────────────────────────


@router.get("/social-sources", response_model=list[SocialSourceResponse])
async def list_social_sources(db: AsyncSession = Depends(get_db)):
    """List all active social sources."""
    stmt = select(SocialSource).where(SocialSource.is_active.is_(True))
    result = await db.execute(stmt)
    return result.scalars().all()


@router.post(
    "/social-sources",
    response_model=SocialSourceResponse,
    status_code=201,
)
async def create_social_source(
    body: SocialSourceCreate,
    db: AsyncSession = Depends(get_db),
):
    """Create a new social source."""
    source = SocialSource(
        platform=body.platform,
        source_type=body.source_type,
        external_id=body.external_id,
        name=body.name,
        url=body.url,
        api_config=body.api_config,
        region=body.region,
        language=body.language,
    )
    db.add(source)
    await db.flush()
    await db.refresh(source)
    return source
