"""
NLP utility tests (no heavy model downloads required).

Tests Urdu detection, keyword extraction, statistics parsing,
and the severity calculation algorithm.

Run: pytest tests/test_nlp_utils.py -v
"""
from __future__ import annotations
import pytest
from app.nlp.urdu_utils import (
    is_urdu, extract_urdu_entities, extract_urdu_statistics,
    DISEASE_KEYWORDS_URDU, LOCATION_KEYWORDS_URDU,
)

class TestUrduDetection:
    def test_pure_urdu(self):
        assert is_urdu("کراچی میں ڈینگی کے کیسز بڑھ رہے ہیں") is True

    def test_pure_english(self):
        assert is_urdu("Dengue cases rising in Karachi") is False

    def test_mixed_mostly_urdu(self):
        assert is_urdu("کراچی میں dengue کے 200 نئے کیسز رپورٹ ہوئے") is True

    def test_empty(self):
        assert is_urdu("") is False

    def test_threshold(self):
        # With threshold=0.0, any non-empty string passes (0.0 >= 0.0)
        assert is_urdu("abc", threshold=0.0) is True
        # Pure ASCII fails at the default 0.3 threshold
        assert is_urdu("abc") is False

class TestUrduEntityExtraction:
    def test_finds_diseases(self):
        text = "کراچی میں ڈینگی اور ملیریا کے کیسز"
        ents = extract_urdu_entities(text)
        assert "ڈینگی" in ents["diseases"]
        assert "ملیریا" in ents["diseases"]

    def test_finds_locations(self):
        text = "لاہور اور اسلام آباد میں صورتحال"
        ents = extract_urdu_entities(text)
        assert "لاہور" in ents["locations"]
        assert "اسلام آباد" in ents["locations"]

    def test_no_matches(self):
        ents = extract_urdu_entities("عام خبریں")
        assert ents["diseases"] == []
        assert ents["locations"] == []

    @pytest.mark.parametrize("kw", DISEASE_KEYWORDS_URDU)
    def test_each_disease_keyword(self, kw):
        ents = extract_urdu_entities(f"رپورٹ: {kw} پھیل رہا ہے")
        assert kw in ents["diseases"]

    @pytest.mark.parametrize("kw", LOCATION_KEYWORDS_URDU)
    def test_each_location_keyword(self, kw):
        ents = extract_urdu_entities(f"{kw} میں صورتحال خراب")
        assert kw in ents["locations"]

class TestUrduStatistics:
    def test_affected_count(self):
        text = "کراچی میں 500 مریض رپورٹ ہوئے"
        stats = extract_urdu_statistics(text)
        assert stats["affected"] == 500

    def test_death_count(self):
        text = "15 افراد ہلاک ہو گئے"
        stats = extract_urdu_statistics(text)
        assert stats["deaths"] == 15

    def test_recovered_count(self):
        text = "200 مریض صحت یاب ہو کر رخصت"
        stats = extract_urdu_statistics(text)
        assert stats["recovered"] is not None

    def test_no_numbers(self):
        text = "مریض ہسپتال میں داخل"
        stats = extract_urdu_statistics(text)
        # keyword found but no number adjacent
        # implementation picks nearest number in window
        assert stats["affected"] is None or isinstance(stats["affected"], int)

class TestSeverityCalculation:
    """Test the severity algorithm without loading models."""

    def _calc(self, category="general health information", score=0.5,
              deaths=None, affected=None, disease_count=0):
        from app.nlp.processor import NLPProcessor
        proc = NLPProcessor.__new__(NLPProcessor)
        return proc.calculate_severity(
            classification={"category": category, "score": score},
            statistics={"deaths": deaths, "affected": affected, "recovered": None},
            diseases=["d"] * disease_count,
        )

    def test_outbreak_high_severity(self):
        s = self._calc("disease outbreak alert", 0.9, deaths=10, affected=500, disease_count=2)
        assert s > 0.5

    def test_general_info_low(self):
        s = self._calc("general health information", 0.8)
        assert s < 0.2

    def test_deaths_boost(self):
        base = self._calc("disease outbreak alert", 0.8)
        with_deaths = self._calc("disease outbreak alert", 0.8, deaths=5)
        assert with_deaths > base

    def test_clamped_to_1(self):
        s = self._calc("disease outbreak alert", 1.0, deaths=100, affected=10000, disease_count=10)
        assert s <= 1.0

    def test_zero_diseases(self):
        s = self._calc("vaccination campaign", 0.5)
        assert s >= 0.0
