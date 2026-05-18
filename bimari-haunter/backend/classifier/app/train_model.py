
# ============================================================
# BimariHunter - Model Training Script
# ============================================================
# Run this script once to train the TF-IDF + Logistic
# Regression classifier and save the model artifacts.
#
# Usage (from project root):
#   python -m app.train_model
# ============================================================

import os
import sys
import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report

# Ensure app package is importable when run directly
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.preprocess import clean_text

# ── Paths ─────────────────────────────────────────────────
DATASET_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "dataset.csv")
MODEL_DIR    = os.path.join(os.path.dirname(__file__), "model")
MODEL_PATH   = os.path.join(MODEL_DIR, "classifier.pkl")
VEC_PATH     = os.path.join(MODEL_DIR, "vectorizer.pkl")

# ── Ensure model directory exists ─────────────────────────
os.makedirs(MODEL_DIR, exist_ok=True)

# ── Load Dataset ──────────────────────────────────────────
print("[1/5] Loading dataset...")
df = pd.read_csv(DATASET_PATH)
print(f"      {len(df)} samples found across {df['label'].nunique()} categories.")

# ── Clean Text ────────────────────────────────────────────
print("[2/5] Cleaning text...")
df["clean_text"] = df["text"].apply(clean_text)

X = df["clean_text"]
y = df["label"]

# ── TF-IDF Vectorization ──────────────────────────────────
print("[3/5] Vectorizing with TF-IDF...")
vectorizer = TfidfVectorizer(
    max_features=5000,
    ngram_range=(1, 2),   # unigrams + bigrams
    sublinear_tf=True
)
X_vec = vectorizer.fit_transform(X)

# ── Train / Test Split ────────────────────────────────────
X_train, X_test, y_train, y_test = train_test_split(
    X_vec, y, test_size=0.2, random_state=42, stratify=y
)

# ── Train Model ───────────────────────────────────────────
print("[4/5] Training Logistic Regression model...")
model = LogisticRegression(
    max_iter=1000,
    C=1.0,
    solver="lbfgs",
)
model.fit(X_train, y_train)

# ── Evaluate ──────────────────────────────────────────────
y_pred = model.predict(X_test)
print("\n-- Classification Report --------------------------")
print(classification_report(y_test, y_pred, zero_division=0))

# ── Save Artifacts ────────────────────────────────────────
print("[5/5] Saving model and vectorizer...")
joblib.dump(model, MODEL_PATH)
joblib.dump(vectorizer, VEC_PATH)
print(f"      Model saved   -> {MODEL_PATH}")
print(f"      Vectorizer    -> {VEC_PATH}")
print("\n[OK] Training complete. Run the API with: uvicorn app.main:app --reload")
