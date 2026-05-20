"""
Pre-configured Facebook community pages for health monitoring.

external_id and access_token are placeholders – replace with real
values via environment variables or Secret Manager at deploy time.
"""

from __future__ import annotations

FACEBOOK_PAGES: list[dict] = [
    {
        "name": "Pakistan Health Watch",
        "platform": "facebook",
        "source_type": "page",
        "external_id": "PLACEHOLDER_PAGE_ID_1",
        "region": "Pakistan",
        "language": "mixed",
        "api_config": {
            "access_token": "PLACEHOLDER_ACCESS_TOKEN",
            "token_type": "page_admin",
            "rate_limit": 200,
        },
    },
    {
        "name": "Dengue Alert Pakistan",
        "platform": "facebook",
        "source_type": "page",
        "external_id": "PLACEHOLDER_PAGE_ID_2",
        "region": "Pakistan",
        "language": "mixed",
        "api_config": {
            "access_token": "PLACEHOLDER_ACCESS_TOKEN",
            "token_type": "page_admin",
            "rate_limit": 200,
        },
    },
    {
        "name": "Disease Surveillance Pakistan",
        "platform": "facebook",
        "source_type": "page",
        "external_id": "PLACEHOLDER_PAGE_ID_3",
        "region": "Pakistan",
        "language": "mixed",
        "api_config": {
            "access_token": "PLACEHOLDER_ACCESS_TOKEN",
            "token_type": "page_admin",
            "rate_limit": 200,
        },
    },
    {
        "name": "Health Department Punjab",
        "platform": "facebook",
        "source_type": "page",
        "external_id": "PLACEHOLDER_PAGE_ID_4",
        "region": "Pakistan",
        "language": "mixed",
        "api_config": {
            "access_token": "PLACEHOLDER_ACCESS_TOKEN",
            "token_type": "page_admin",
            "rate_limit": 200,
        },
    },
    {
        "name": "Sindh Health Department",
        "platform": "facebook",
        "source_type": "page",
        "external_id": "PLACEHOLDER_PAGE_ID_5",
        "region": "Pakistan",
        "language": "mixed",
        "api_config": {
            "access_token": "PLACEHOLDER_ACCESS_TOKEN",
            "token_type": "page_admin",
            "rate_limit": 200,
        },
    },
]
