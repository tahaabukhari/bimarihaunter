
# BimariHunter 🩺🔍
### AI-Powered Crisis Text Classification System

BimariHunter automatically classifies news articles and social media posts into **8 predefined crisis categories** using a two-stage pipeline:

1. **Keyword Filter** — Fast rule-based matching
2. **ML Classifier** — TF-IDF + Logistic Regression

---

## 📁 Project Structure

```
bimarihunter/
├── app/
│   ├── __init__.py
│   ├── main.py           ← FastAPI REST API
│   ├── classifier.py     ← ML inference module
│   ├── keyword_filter.py ← Rule-based keyword matcher
│   ├── preprocess.py     ← Text cleaning utilities
│   ├── train_model.py    ← Model training script
│   ├── categories.py     ← Crisis category definitions
│   └── model/
│       ├── classifier.pkl   (generated after training)
│       └── vectorizer.pkl   (generated after training)
│
├── data/
│   └── dataset.csv       ← Labeled training data (50+ samples)
│
├── classification.py     ← ⭐ ALL-IN-ONE consolidated file
├── requirements.txt
└── README.md
```

---

## 🏷️ Categories

| # | Category | Examples |
|---|---|---|
| 1 | Disease Outbreak | Virus, epidemic, quarantine |
| 2 | Natural Disaster | Flood, earthquake, cyclone |
| 3 | Infrastructure Failure | Power outage, bridge collapse |
| 4 | Accidents/Emergencies | Explosion, chemical spill |
| 5 | Community Response/Aid | Volunteers, donations, shelter |
| 6 | Prevention/Safety Tips | Guidelines, first aid, precautions |
| 7 | Pharmacy/Cost Updates | Drug prices, medication shortage |
| 8 | General News | Government announcements, events |

---

## ⚡ Quick Start

### 1. Install dependencies
```bash
pip install -r requirements.txt
```

### 2. Train the ML model
```bash
# Option A: Using the modular app
python -m app.train_model

# Option B: Using the all-in-one file
python classification.py --train
```

### 3. Start the API server
```bash
# Option A: Modular app
uvicorn app.main:app --reload

# Option B: All-in-one file
uvicorn classification:app --reload
```

### 4. Run a quick demo (no API needed)
```bash
python classification.py --demo
```

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Health check |
| GET | `/health` | Service status |
| GET | `/categories` | List all categories |
| POST | `/classify_news` | Classify single text |
| POST | `/classify_batch` | Classify up to 100 texts |

### Example Request
```bash
curl -X POST http://localhost:8000/classify_news \
  -H "Content-Type: application/json" \
  -d '{"text": "Massive flood destroys hundreds of homes"}'
```

### Example Response
```json
{
  "input_text": "Massive flood destroys hundreds of homes",
  "keyword_matches": {"Natural Disaster": 1},
  "best_keyword_match": "Natural Disaster",
  "prediction": "Natural Disaster",
  "confidence": 0.89
}
```

### Interactive Docs
Visit `http://localhost:8000/docs` for Swagger UI.

---

## 🏗️ Architecture

```
News / Social Media Post
        ↓
FastAPI Endpoint (/classify_news)
        ↓
Keyword Filter (fast rule-based)
        ↓
ML Text Classifier (TF-IDF + LogReg)
        ↓
Predicted Category + Confidence Score
        ↓
Dashboard / Alerts / Notifications
```

---

## 📊 Model Details

- **Vectorizer**: TF-IDF (max 5000 features, unigrams + bigrams)
- **Classifier**: Logistic Regression (multinomial, max_iter=1000)
- **Train/Test Split**: 80/20 with stratification
- **Dataset**: 50 labeled samples across 8 categories

---

## 📦 All-in-One File

`classification.py` (project root) is a self-contained file containing **all 7 sections**:
```
Section 1 │ CATEGORIES     – Crisis category definitions
Section 2 │ PREPROCESS     – Text cleaning utilities  
Section 3 │ KEYWORD FILTER – Rule-based keyword matcher
Section 4 │ TRAIN MODEL    – TF-IDF + LogReg trainer
Section 5 │ CLASSIFIER     – ML inference wrapper
Section 6 │ FASTAPI APP    – REST API endpoints
Section 7 │ DEMO / MAIN    – CLI demo & entrypoint
```
