
# ============================================================
# BimariHunter - Keyword Filtering System
# ============================================================
# Performs fast, rule-based category matching against the
# predefined keyword lists in categories.py.
# Used as a lightweight pre-filter before the ML model.
# ============================================================

from app.categories import CATEGORIES


def keyword_match(text: str) -> dict:
    """
    Scan text for keyword matches across all crisis categories.

    Args:
        text (str): Raw input text (news article or social post).

    Returns:
        dict: {category_name: match_count} for categories with
              at least one keyword match. Empty dict if no match.
    """
    text = text.lower()
    matches = {}

    for category, keywords in CATEGORIES.items():
        count = sum(1 for keyword in keywords if keyword in text)
        if count > 0:
            matches[category] = count

    return matches


def best_keyword_category(text: str) -> str | None:
    """
    Return the top keyword-matched category (highest count),
    or None if no keywords match at all.

    Args:
        text (str): Raw input text.

    Returns:
        str | None: Category name or None.
    """
    matches = keyword_match(text)
    if not matches:
        return None
    return max(matches, key=matches.get)
