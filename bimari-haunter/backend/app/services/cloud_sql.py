"""
Cloud SQL connection helper for Google Cloud Run.

When running on Cloud Run, Cloud SQL is accessed via a Unix domain
socket mounted by the Cloud SQL Auth Proxy sidecar.  This module
builds the correct connection string automatically.
"""

from __future__ import annotations

import os


def get_cloud_sql_url(
    *,
    user: str | None = None,
    password: str | None = None,
    db_name: str | None = None,
    instance_connection: str | None = None,
    driver: str = "asyncpg",
) -> str:
    """Build an asyncpg-compatible connection URL for Cloud SQL.

    Parameters are read from env-vars if not passed explicitly:
        DB_USER, DB_PASS, DB_NAME, INSTANCE_CONNECTION_NAME

    Returns a URL of the form:
        postgresql+asyncpg://user:pass@/dbname?unix_sock=/cloudsql/PROJECT:REGION:INSTANCE/.s.PGSQL.5432
    """
    user = user or os.getenv("DB_USER", "postgres")
    password = password or os.getenv("DB_PASS", "")
    db_name = db_name or os.getenv("DB_NAME", "bimari_haunter")
    instance_connection = instance_connection or os.getenv(
        "INSTANCE_CONNECTION_NAME", ""
    )

    socket_path = f"/cloudsql/{instance_connection}/.s.PGSQL.5432"

    return (
        f"postgresql+{driver}://{user}:{password}@/{db_name}"
        f"?unix_sock={socket_path}"
    )


def is_cloud_run() -> bool:
    """Return True when the process is running inside Cloud Run."""
    return bool(os.getenv("K_SERVICE"))
