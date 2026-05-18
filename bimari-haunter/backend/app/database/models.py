"""
SQLAlchemy 2.0 ORM models for BimariHaunter.

Every table, column, constraint, and index listed in the spec is
defined here using the declarative-mapping style.
"""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    DateTime,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    ForeignKey,
    func,
)
from sqlalchemy.dialects.postgresql import ARRAY, JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


# ── Base ────────────────────────────────────────────────────

class Base(DeclarativeBase):
    pass


# ── Core tables ─────────────────────────────────────────────

class NewsSource(Base):
    __tablename__ = "news_sources"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    base_url: Mapped[str] = mapped_column(String(500), nullable=False)
    region: Mapped[str | None] = mapped_column(String(100), nullable=True)
    country: Mapped[str] = mapped_column(String(100), nullable=False, server_default="Pakistan")
    language: Mapped[str] = mapped_column(String(50), server_default="mixed")
    source_type: Mapped[str] = mapped_column(String(20), server_default="web")
    scrape_config: Mapped[dict] = mapped_column(JSONB, nullable=False, server_default="{}")
    schedule: Mapped[str] = mapped_column(String(50), server_default="0 */6 * * *")
    is_active: Mapped[bool] = mapped_column(Boolean, server_default="true")
    last_scraped_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relationships
    articles: Mapped[list["RawArticle"]] = relationship(back_populates="source")


class SocialSource(Base):
    __tablename__ = "social_sources"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    platform: Mapped[str] = mapped_column(String(50), nullable=False)
    source_type: Mapped[str] = mapped_column(String(50), nullable=False)
    external_id: Mapped[str] = mapped_column(String(255), nullable=False)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    api_config: Mapped[dict] = mapped_column(JSONB, nullable=False, server_default="{}")
    region: Mapped[str | None] = mapped_column(String(100), nullable=True)
    language: Mapped[str] = mapped_column(String(50), server_default="mixed")
    is_active: Mapped[bool] = mapped_column(Boolean, server_default="true")
    last_fetched_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    social_posts: Mapped[list["RawSocialPost"]] = relationship(back_populates="source")


class ScrapeJob(Base):
    __tablename__ = "scrape_jobs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    source_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    source_type: Mapped[str] = mapped_column(String(20), nullable=False)
    job_type: Mapped[str] = mapped_column(String(20), nullable=False)
    status: Mapped[str] = mapped_column(String(20), nullable=False, server_default="pending")
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    items_found: Mapped[int] = mapped_column(Integer, server_default="0")
    items_stored: Mapped[int] = mapped_column(Integer, server_default="0")
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    metadata_: Mapped[dict] = mapped_column("metadata", JSONB, server_default="{}")
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


# ── Raw data tables ─────────────────────────────────────────

class RawArticle(Base):
    __tablename__ = "raw_articles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    source_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("news_sources.id"), nullable=True)
    job_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("scrape_jobs.id"), nullable=True)
    url: Mapped[str] = mapped_column(String(1000), nullable=False)
    title: Mapped[str | None] = mapped_column(String(500), nullable=True)
    raw_html: Mapped[str | None] = mapped_column(Text, nullable=True)
    extracted_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    published_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    scraped_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    content_hash: Mapped[str | None] = mapped_column(String(64), unique=True, nullable=True)
    processing_status: Mapped[str] = mapped_column(String(20), server_default="pending")
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    source: Mapped["NewsSource | None"] = relationship(back_populates="articles")

    __table_args__ = (
        Index("idx_articles_status", "processing_status"),
    )


class RawSocialPost(Base):
    __tablename__ = "raw_social_posts"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    source_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("social_sources.id"), nullable=True)
    job_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("scrape_jobs.id"), nullable=True)
    platform: Mapped[str] = mapped_column(String(50), nullable=False)
    external_post_id: Mapped[str] = mapped_column(String(255), nullable=False)
    permalink: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    raw_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    cleaned_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    media_urls: Mapped[list[str] | None] = mapped_column(ARRAY(Text), nullable=True)
    post_type: Mapped[str | None] = mapped_column(String(50), nullable=True)
    author_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    author_id: Mapped[str | None] = mapped_column(String(255), nullable=True)
    published_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    scraped_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    likes_count: Mapped[int] = mapped_column(Integer, server_default="0")
    comments_count: Mapped[int] = mapped_column(Integer, server_default="0")
    shares_count: Mapped[int] = mapped_column(Integer, server_default="0")
    content_hash: Mapped[str | None] = mapped_column(String(64), unique=True, nullable=True)
    processing_status: Mapped[str] = mapped_column(String(20), server_default="pending")
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    source: Mapped["SocialSource | None"] = relationship(back_populates="social_posts")

    __table_args__ = (
        UniqueConstraint("platform", "external_post_id", name="uq_social_platform_post"),
        Index("idx_social_status", "processing_status"),
    )


# ── NLP table ───────────────────────────────────────────────

class ExtractedEntity(Base):
    __tablename__ = "extracted_entities"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    source_table: Mapped[str] = mapped_column(String(20), nullable=False)
    source_id: Mapped[int] = mapped_column(Integer, nullable=False)
    diseases: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    symptoms: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    locations: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    organizations: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    people: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    dates: Mapped[list[datetime] | None] = mapped_column(ARRAY(DateTime), nullable=True)
    affected_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    death_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    model_used: Mapped[str | None] = mapped_column(String(100), nullable=True)
    confidence: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    processing_time_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    insights: Mapped[list["ReportV1Insight"]] = relationship(back_populates="entity")
    full_reports: Mapped[list["ReportV2Full"]] = relationship(back_populates="entity")

    __table_args__ = (
        Index("idx_entities_source", "source_table", "source_id"),
    )


# ── Report Version A – Insights ────────────────────────────

class ReportV1Insight(Base):
    __tablename__ = "reports_v1_insights"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    entity_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("extracted_entities.id"), nullable=True)
    source_type: Mapped[str] = mapped_column(String(20), nullable=False)
    source_table: Mapped[str] = mapped_column(String(20), nullable=False)
    source_record_id: Mapped[int] = mapped_column(Integer, nullable=False)
    headline: Mapped[str] = mapped_column(String(300), nullable=False)
    summary: Mapped[str] = mapped_column(Text, nullable=False)
    key_points: Mapped[dict] = mapped_column(JSONB, nullable=False)
    category: Mapped[str] = mapped_column(String(50), nullable=False)
    severity_score: Mapped[Decimal] = mapped_column(Numeric(3, 2), nullable=False)
    urgency_level: Mapped[str | None] = mapped_column(String(20), nullable=True)
    primary_location: Mapped[str | None] = mapped_column(String(100), nullable=True)
    geo_json: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    relevance_score: Mapped[Decimal] = mapped_column(Numeric(3, 2), server_default="0.5")
    freshness_score: Mapped[Decimal] = mapped_column(Numeric(3, 2), server_default="0.5")
    event_date: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    published_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    delivery_status: Mapped[str] = mapped_column(String(20), server_default="pending")
    delivered_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    read_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relationships
    entity: Mapped["ExtractedEntity | None"] = relationship(back_populates="insights")
    deliveries: Mapped[list["DeliveryQueue"]] = relationship(
        back_populates="report_v1",
        foreign_keys="DeliveryQueue.report_v1_id",
    )

    __table_args__ = (
        Index("idx_v1_location", "primary_location"),
        Index("idx_v1_category", "category", "severity_score"),
        Index("idx_v1_delivery", "delivery_status", "created_at"),
    )


# ── Report Version B – Full Reports ────────────────────────

class ReportV2Full(Base):
    __tablename__ = "reports_v2_full"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    entity_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("extracted_entities.id"), nullable=True)
    source_type: Mapped[str] = mapped_column(String(20), nullable=False)
    source_table: Mapped[str] = mapped_column(String(20), nullable=False)
    source_record_id: Mapped[int] = mapped_column(Integer, nullable=False)
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    full_text: Mapped[str] = mapped_column(Text, nullable=False)
    summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    source_url: Mapped[str] = mapped_column(String(1000), nullable=False)
    source_name: Mapped[str] = mapped_column(String(255), nullable=False)
    source_logo_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    author_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    media_urls: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    diseases: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    symptoms: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    locations: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    affected_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    death_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    recovered_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    category: Mapped[str | None] = mapped_column(String(50), nullable=True)
    severity_score: Mapped[Decimal | None] = mapped_column(Numeric(3, 2), nullable=True)
    confidence: Mapped[Decimal | None] = mapped_column(Numeric(3, 2), nullable=True)
    primary_location: Mapped[str | None] = mapped_column(String(100), nullable=True)
    all_locations: Mapped[list[str] | None] = mapped_column(ARRAY(String(255)), nullable=True)
    geo_json: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    verified: Mapped[bool] = mapped_column(Boolean, server_default="false")
    verified_by: Mapped[str | None] = mapped_column(String(255), nullable=True)
    verified_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    event_date: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    published_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    delivery_status: Mapped[str] = mapped_column(String(20), server_default="pending")
    delivered_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    read_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relationships
    entity: Mapped["ExtractedEntity | None"] = relationship(back_populates="full_reports")
    deliveries: Mapped[list["DeliveryQueue"]] = relationship(
        back_populates="report_v2",
        foreign_keys="DeliveryQueue.report_v2_id",
    )

    __table_args__ = (
        Index("idx_v2_location", "primary_location"),
        Index("idx_v2_disease", "diseases", postgresql_using="gin"),
        Index("idx_v2_delivery", "delivery_status", "created_at"),
    )


# ── Delivery tables ─────────────────────────────────────────

class AppSession(Base):
    __tablename__ = "app_sessions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(255), nullable=False, unique=True)
    fcm_token: Mapped[str | None] = mapped_column(String(500), nullable=True)
    user_preferences: Mapped[dict] = mapped_column(JSONB, server_default="{}")
    last_active_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    deliveries: Mapped[list["DeliveryQueue"]] = relationship(back_populates="session")

    __table_args__ = (
        Index("idx_app_sessions_device", "device_id"),
    )


class DeliveryQueue(Base):
    __tablename__ = "delivery_queue"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    session_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("app_sessions.id"), nullable=True)
    report_v1_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("reports_v1_insights.id"), nullable=True)
    report_v2_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("reports_v2_full.id"), nullable=True)
    priority: Mapped[int] = mapped_column(Integer, server_default="5")
    status: Mapped[str] = mapped_column(String(20), server_default="queued")
    retry_count: Mapped[int] = mapped_column(Integer, server_default="0")
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    sent_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    delivered_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    # Relationships
    session: Mapped["AppSession | None"] = relationship(back_populates="deliveries")
    report_v1: Mapped["ReportV1Insight | None"] = relationship(
        back_populates="deliveries",
        foreign_keys=[report_v1_id],
    )
    report_v2: Mapped["ReportV2Full | None"] = relationship(
        back_populates="deliveries",
        foreign_keys=[report_v2_id],
    )

    __table_args__ = (
        Index("idx_queue_status", "status", "priority"),
    )
