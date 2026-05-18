"""
Application configuration via pydantic-settings.
Loads from environment variables / .env file.
"""

from __future__ import annotations

from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Central settings consumed by every layer of the application."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # ── Database ────────────────────────────────────────────
    database_url: str = "postgresql+asyncpg://user:pass@localhost/bimari_haunter"

    # ── Facebook Graph API ──────────────────────────────────
    facebook_app_id: str = ""
    facebook_app_secret: str = ""
    facebook_access_token: str = ""

    # ── Google Cloud ────────────────────────────────────────
    google_cloud_project: str = ""
    secret_manager_prefix: str = "bimari-haunter"

    # ── Optional services ───────────────────────────────────
    redis_url: Optional[str] = None

    # ── Operational limits ──────────────────────────────────
    max_concurrent_scrapers: int = 5
    websocket_max_connections: int = 1000


settings = Settings()
