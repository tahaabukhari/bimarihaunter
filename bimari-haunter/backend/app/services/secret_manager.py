"""
Google Cloud Secret Manager client.

Fetches secrets at startup or on-demand so that credentials are never
stored in code or environment variables on disk.
"""

from __future__ import annotations

import functools
from typing import Optional

import structlog

from app.config import settings

logger = structlog.get_logger(__name__)

# Lazy-loaded client
_client = None


def _get_client():
    global _client
    if _client is None:
        try:
            from google.cloud import secretmanager

            _client = secretmanager.SecretManagerServiceClient()
        except Exception as exc:
            logger.warning("secret_manager_unavailable", error=str(exc))
    return _client


def get_secret(
    secret_id: str,
    *,
    version: str = "latest",
    project: Optional[str] = None,
) -> Optional[str]:
    """Fetch a secret payload from GCP Secret Manager.

    The full resource name is assembled as:
        projects/{project}/secrets/{prefix}-{secret_id}/versions/{version}

    Returns the decoded UTF-8 payload, or None if the client is
    unavailable (e.g. running locally without GCP credentials).
    """
    client = _get_client()
    if client is None:
        logger.debug("secret_manager_skipped", secret_id=secret_id)
        return None

    project = project or settings.google_cloud_project
    prefix = settings.secret_manager_prefix
    name = f"projects/{project}/secrets/{prefix}-{secret_id}/versions/{version}"

    try:
        response = client.access_secret_version(request={"name": name})
        payload = response.payload.data.decode("utf-8")
        logger.info("secret_fetched", secret_id=secret_id)
        return payload
    except Exception as exc:
        logger.error("secret_fetch_failed", secret_id=secret_id, error=str(exc))
        return None


@functools.lru_cache(maxsize=32)
def get_secret_cached(secret_id: str) -> Optional[str]:
    """Same as get_secret but results are cached for the process lifetime."""
    return get_secret(secret_id)
