"""
Maps endpoints — geolocated outbreak markers and Gemini-powered insights.

Endpoints:
  GET /api/v1/maps/markers  — Returns map markers from Firestore /reports
  GET /api/v1/maps/insights — Returns Gemini-generated insights from recent reports
"""
from __future__ import annotations

import json
import os
from datetime import datetime
from typing import Any, Dict, List, Optional

import structlog
from fastapi import APIRouter, Depends, HTTPException, Query

from app.database.firestore import db
from app.services.firebase_auth import verify_firebase_token
from app.config import settings

logger = structlog.get_logger(__name__)
router = APIRouter(prefix="/maps", tags=["maps"])


def _serialize_marker(doc_id: str, data: dict, analysis: dict, coords) -> dict:
    pub_at = data.get("published_at")
    pub_at_str = pub_at.isoformat() if isinstance(pub_at, datetime) else str(pub_at or "")
    return {
        "id": doc_id,
        "title": data.get("title", "Outbreak Report"),
        "disease": analysis.get("disease", "outbreak"),
        "severity": analysis.get("severity", "medium"),
        "latitude": coords.latitude,
        "longitude": coords.longitude,
        "summary": analysis.get("summary", []),
        "symptoms": analysis.get("symptoms", []),
        "locations": analysis.get("locations", []),
        "source": data.get("source", "BimariHaunter"),
        "published_at": pub_at_str,
        "confidence_score": analysis.get("confidence_score", 0.8),
    }


@router.get("/markers")
async def get_map_markers(
    disease: Optional[str] = Query(None, description="Filter by disease (e.g. dengue)"),
    severity: Optional[str] = Query(None, description="Filter by severity: high, medium, low"),
    limit: int = Query(100, ge=1, le=300),
    user_token: dict = Depends(verify_firebase_token),
) -> List[Dict[str, Any]]:
    """
    Returns geolocated outbreak markers from Firestore for the Android map.
    Handles missing indexes by falling back to simpler queries.
    """
    markers: list[dict] = []

    # ── Strategy 1: analyzed reports (no ordering to avoid index issues) ──
    try:
        docs = list(
            db.collection("reports")
            .where("status", "==", "analyzed")
            .limit(limit * 3)
            .stream()
        )
    except Exception:
        # ── Strategy 2: all reports, no filter ────────────────────────────
        try:
            docs = list(db.collection("reports").limit(limit * 3).stream())
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Map markers unavailable: {str(e)}")

    for doc in docs:
        data = doc.to_dict()
        analysis = data.get("ai_analysis")
        if not isinstance(analysis, dict):
            continue

        coords = analysis.get("coordinates")
        if not coords or not hasattr(coords, "latitude"):
            continue

        rep_disease = analysis.get("disease", "")
        if disease and disease.lower() not in rep_disease.lower():
            continue

        rep_severity = analysis.get("severity", "medium")
        if severity and severity.lower() != rep_severity.lower():
            continue

        markers.append(_serialize_marker(doc.id, data, analysis, coords))
        if len(markers) >= limit:
            break

    logger.info("map_markers_served", count=len(markers))
    return markers


@router.get("/insights")
async def get_map_insights(
    city: Optional[str] = Query(None, description="City to generate insights for"),
    user_token: dict = Depends(verify_firebase_token),
) -> Dict[str, Any]:
    """
    Returns Gemini-powered insights derived from recent Firestore reports.
    Includes disease breakdown, severity stats, top affected areas, and recommendations.
    Falls back to rule-based insights if Gemini is unavailable.
    """
    # ── 1. Fetch recent analyzed reports ──────────────────────────────────
    try:
        docs = list(
            db.collection("reports")
            .where("status", "==", "analyzed")
            .limit(50)
            .stream()
        )
    except Exception:
        docs = list(db.collection("reports").limit(50).stream())

    if not docs:
        return _empty_insights(city)

    # ── 2. Aggregate data for Gemini prompt ───────────────────────────────
    disease_counts: dict[str, int] = {}
    severity_counts: dict[str, int] = {"high": 0, "medium": 0, "low": 0}
    city_counts: dict[str, int] = {}
    report_summaries: list[str] = []

    for doc in docs:
        data = doc.to_dict()
        ai = data.get("ai_analysis") or {}
        if not isinstance(ai, dict):
            continue

        disease = ai.get("disease", "unknown")
        disease_counts[disease] = disease_counts.get(disease, 0) + 1

        sev = ai.get("severity", "medium")
        if sev in severity_counts:
            severity_counts[sev] += 1

        for loc in (ai.get("locations") or []):
            city_counts[loc] = city_counts.get(loc, 0) + 1

        summary = ai.get("summary", [])
        if summary:
            first = summary[0] if isinstance(summary, list) else str(summary)
            report_summaries.append(first[:200])

    total = len(docs)
    top_disease = max(disease_counts, key=disease_counts.get) if disease_counts else "dengue"
    top_cities = sorted(city_counts.items(), key=lambda x: x[1], reverse=True)[:5]
    risk_level = "high" if severity_counts["high"] > total * 0.3 else \
                 "medium" if severity_counts["medium"] > total * 0.3 else "low"

    # ── 3. Try Gemini for intelligent insights ────────────────────────────
    gemini_insights: dict | None = None
    try:
        api_key = settings.gemini_api_key or os.environ.get("GEMINI_API_KEY", "")
        if api_key:
            import google.generativeai as genai
            genai.configure(api_key=api_key)
            model = genai.GenerativeModel("gemini-1.5-flash")

            summaries_text = "\n".join(f"- {s}" for s in report_summaries[:10])
            disease_text = ", ".join(f"{d}: {c} reports" for d, c in disease_counts.items())
            city_text = ", ".join(f"{c}: {n} reports" for c, n in top_cities)
            city_context = f" for {city}" if city else " across Pakistan"

            prompt = (
                f"You are BimariHaunter, a public health intelligence system for Pakistan. "
                f"Based on the following outbreak data{city_context}, generate a concise JSON insights report.\n\n"
                f"Disease breakdown: {disease_text}\n"
                f"Top affected areas: {city_text}\n"
                f"Risk level: {risk_level}\n"
                f"Recent report summaries:\n{summaries_text}\n\n"
                f"Return ONLY a valid JSON object with these exact keys:\n"
                f"- \"trend_summary\": 2-sentence summary of current outbreak trends\n"
                f"- \"recommendations\": list of 3 specific actionable recommendations for residents\n"
                f"- \"weekly_prediction\": 1-sentence prediction for next 7 days\n"
                f"- \"risk_assessment\": 1-sentence overall risk assessment\n"
                f"Do not wrap in markdown. Return raw JSON only."
            )

            response = model.generate_content(prompt)
            text = response.text.strip()
            if text.startswith("```"):
                lines = text.splitlines()
                text = "\n".join(lines[1:-1] if lines[-1] == "```" else lines[1:]).strip()
            gemini_insights = json.loads(text)
    except Exception as e:
        logger.warning("gemini_insights_failed_using_fallback", error=str(e))

    # ── 4. Build response ─────────────────────────────────────────────────
    return {
        "total_reports": total,
        "risk_level": risk_level,
        "top_disease": top_disease,
        "disease_breakdown": [
            {"disease": d, "count": c, "percentage": round(c / total * 100)}
            for d, c in sorted(disease_counts.items(), key=lambda x: x[1], reverse=True)
        ],
        "severity_breakdown": severity_counts,
        "top_affected_areas": [
            {"city": c, "report_count": n} for c, n in top_cities
        ],
        "trend_summary": gemini_insights.get("trend_summary", _fallback_trend(top_disease, risk_level))
            if gemini_insights else _fallback_trend(top_disease, risk_level),
        "recommendations": gemini_insights.get("recommendations", _fallback_recommendations(top_disease))
            if gemini_insights else _fallback_recommendations(top_disease),
        "weekly_prediction": gemini_insights.get("weekly_prediction",
            f"Continued monitoring of {top_disease} cases is recommended over the next 7 days.")
            if gemini_insights else f"Continued monitoring of {top_disease} cases is recommended over the next 7 days.",
        "risk_assessment": gemini_insights.get("risk_assessment",
            f"Overall risk is {risk_level}. {severity_counts['high']} high-severity reports detected.")
            if gemini_insights else f"Overall risk is {risk_level}. {severity_counts['high']} high-severity reports detected.",
        "powered_by": "gemini-1.5-flash" if gemini_insights else "rule-based-v1",
        "generated_at": datetime.utcnow().isoformat(),
    }


def _empty_insights(city: str | None) -> dict:
    return {
        "total_reports": 0,
        "risk_level": "low",
        "top_disease": "none",
        "disease_breakdown": [],
        "severity_breakdown": {"high": 0, "medium": 0, "low": 0},
        "top_affected_areas": [],
        "trend_summary": "No outbreak data available yet. Trigger a scrape job to populate reports.",
        "recommendations": ["Stay hydrated", "Wash hands regularly", "Monitor local health advisories"],
        "weekly_prediction": "Insufficient data for prediction.",
        "risk_assessment": "No data available.",
        "powered_by": "none",
        "generated_at": datetime.utcnow().isoformat(),
    }


def _fallback_trend(disease: str, risk: str) -> str:
    return (
        f"Current data shows elevated {disease} activity with {risk} overall risk. "
        f"Residents in affected areas should take preventive measures."
    )


def _fallback_recommendations(disease: str) -> list[str]:
    base = ["Wash hands frequently with soap and water", "Stay hydrated and maintain good nutrition"]
    disease_specific = {
        "dengue": "Eliminate stagnant water sources and use mosquito repellent",
        "cholera": "Drink only boiled or purified water and avoid raw foods",
        "malaria": "Use mosquito nets and wear long-sleeved clothing at dusk",
        "typhoid": "Avoid street food and ensure all water is purified before drinking",
        "covid": "Wear masks in crowded places and maintain social distancing",
        "influenza": "Get vaccinated and avoid close contact with symptomatic individuals",
    }
    specific = disease_specific.get(disease, "Consult a healthcare provider if symptoms develop")
    return base + [specific]
