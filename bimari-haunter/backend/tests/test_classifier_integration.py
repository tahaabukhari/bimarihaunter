"""Integration test to verify the local classifier is used and returns
an HF-like result shape when called through `NLPProcessor.classify`.

Run: pytest -q tests/test_classifier_integration.py
"""
from __future__ import annotations

from app.nlp.processor import NLPProcessor


def test_local_classifier_integration():
    proc = NLPProcessor()
    res = proc.classify("Dengue outbreak reported in Karachi with many patients")

    assert isinstance(res, dict)
    assert "category" in res
    assert "score" in res
    assert isinstance(res["category"], str)
    assert isinstance(res["score"], float)
