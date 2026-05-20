"""
Application configuration via pydantic-settings.
Loads from environment variables / .env file.
"""
from __future__ import annotations
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Central settings consumed by every layer of the application."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Override via GEMINI_API_KEY environment variable in Cloud Run
    gemini_api_key: str = "AIzaSyDpJ_0HG5zNQxoMciTkwlIIz0cFNaV1rwA"

    # Operational limits
    max_concurrent_scrapers: int = 5


settings = Settings()
