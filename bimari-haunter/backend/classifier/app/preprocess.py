
# ============================================================
# BimariHunter - Text Preprocessing Module
# ============================================================
# Cleans raw text before ML vectorization:
#   - Lowercasing
#   - URL removal
#   - Special character removal
#   - Stopword filtering
# ============================================================

import re
import nltk
from nltk.corpus import stopwords

# Download required NLTK data (runs once, cached after that)
nltk.download("stopwords", quiet=True)
nltk.download("punkt", quiet=True)

stop_words = set(stopwords.words("english"))


def clean_text(text: str) -> str:
    """
    Preprocess a raw text string for ML classification.

    Steps:
      1. Lowercase the text
      2. Remove URLs
      3. Remove non-alphabetic characters
      4. Strip English stopwords

    Args:
        text (str): Raw input string.

    Returns:
        str: Cleaned, space-joined token string.
    """
    if not isinstance(text, str):
        return ""

    # 1. Lowercase
    text = text.lower()

    # 2. Remove URLs
    text = re.sub(r"http\S+|www\.\S+", "", text)

    # 3. Remove non-alphabet characters (keep spaces)
    text = re.sub(r"[^a-zA-Z ]", "", text)

    # 4. Tokenize and remove stopwords
    words = text.split()
    words = [w for w in words if w not in stop_words and len(w) > 1]

    return " ".join(words)
