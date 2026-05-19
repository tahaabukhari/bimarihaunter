import os
import uuid
import structlog
from datetime import datetime, timezone
from typing import Any, Optional, Dict, List
from app.config import settings

logger = structlog.get_logger(__name__)

class MockDocument:
    def __init__(self, doc_id: str, data: Optional[Dict[str, Any]]):
        self.id = doc_id
        self._data = data

    def to_dict(self) -> Optional[Dict[str, Any]]:
        return self._data

    @property
    def exists(self) -> bool:
        return self._data is not None

class MockQuery:
    def __init__(self, collection_name: str, docs: List[MockDocument]):
        self.collection_name = collection_name
        self.docs = docs

    def where(self, field: str, op: str, value: Any) -> 'MockQuery':
        if value is None:
            return self
        filtered = []
        for d in self.docs:
            data = d.to_dict() or {}
            val = data.get(field)
            if op == "==":
                if val == value:
                    filtered.append(d)
            elif op == ">=":
                if val is not None and val >= value:
                    filtered.append(d)
            elif op == "<=":
                if val is not None and val <= value:
                    filtered.append(d)
        return MockQuery(self.collection_name, filtered)

    def order_by(self, field: str, direction: str = "ASCENDING") -> 'MockQuery':
        reverse = (direction == "DESCENDING" or direction == "descending")
        def sort_key(d):
            val = (d.to_dict() or {}).get(field)
            return val if val is not None else ""
        sorted_docs = sorted(self.docs, key=sort_key, reverse=reverse)
        return MockQuery(self.collection_name, sorted_docs)

    def limit(self, val: int) -> 'MockQuery':
        return MockQuery(self.collection_name, self.docs[:val])

    def offset(self, val: int) -> 'MockQuery':
        return MockQuery(self.collection_name, self.docs[val:])

    def stream(self) -> List[MockDocument]:
        return self.docs

class MockCollection:
    def __init__(self, name: str, db: 'MockFirestoreClient'):
        self.name = name
        self.db = db

    def document(self, doc_id: Optional[str] = None) -> 'MockDocumentReference':
        if doc_id is None:
            doc_id = str(uuid.uuid4())
        return MockDocumentReference(self.name, doc_id, self.db)

    def stream(self) -> List[MockDocument]:
        return [MockDocument(k, v) for k, v in self.db._store.get(self.name, {}).items()]

    def where(self, field: str, op: str, value: Any) -> MockQuery:
        docs = self.stream()
        return MockQuery(self.name, docs).where(field, op, value)

    def order_by(self, field: str, direction: str = "ASCENDING") -> MockQuery:
        docs = self.stream()
        return MockQuery(self.name, docs).order_by(field, direction)

class MockDocumentReference:
    def __init__(self, collection_name: str, doc_id: str, db: 'MockFirestoreClient'):
        self.collection_name = collection_name
        self.id = doc_id
        self.db = db

    def set(self, data: Dict[str, Any]) -> None:
        if self.collection_name not in self.db._store:
            self.db._store[self.collection_name] = {}
        # Make a copy and store it
        self.db._store[self.collection_name][self.id] = dict(data)

    def get(self) -> MockDocument:
        col = self.db._store.get(self.collection_name, {})
        data = col.get(self.id)
        return MockDocument(self.id, data)

class MockFirestoreClient:
    def __init__(self):
        self._store: Dict[str, Dict[str, Dict[str, Any]]] = {}
        self._seed_sample_data()

    def collection(self, name: str) -> MockCollection:
        if name not in self._store:
            self._store[name] = {}
        return MockCollection(name, self)

    def _seed_sample_data(self):
        """Pre-populate the local database with rich Pakistani crisis reports."""
        articles = [
            {
                "id": "e44d3204-c5a4-44b2-8417-64299b9cf2a1",
                "title": "Severe Dengue Outbreak Reported in Lahore, Over 200 Cases Confirmed",
                "body": "Health authorities in Lahore have issued a high alert after a severe dengue outbreak was reported in the city. Over 200 cases have been confirmed within the last 48 hours, mostly concentrated in the Gulberg and Samanabad areas. Hospitals are experiencing a high volume of patients seeking treatment for high fever and joint pain. The local government has initiated spray campaigns and advised citizens to take prevention guidelines seriously, such as eliminating standing water around their homes.",
                "source": "news",
                "category": "Disease Outbreak",
                "confidence": 0.92,
                "region": "Lahore",
                "timestamp": datetime(2026, 5, 19, 12, 0, 0, tzinfo=timezone.utc).isoformat(),
                "url": "https://tribune.com.pk/story/lahore-dengue-outbreak"
            },
            {
                "id": "f55d3205-d6b5-55c3-9528-75399c9df3b2",
                "title": "Flash Floods Inundate Karachi Streets After Record Rain",
                "body": "Monsoon rains have caused severe flash flooding across major parts of Karachi. Main roads are completely submerged, causing severe infrastructure failures and electricity blackouts. Rescue volunteers and aid workers are actively distributing donations and food items in relief camps. High caution is advised as water levels continue to rise near Clifton and Nazimabad.",
                "source": "twitter",
                "category": "Natural Disaster",
                "confidence": 0.88,
                "region": "Karachi",
                "timestamp": datetime(2026, 5, 18, 15, 30, 0, tzinfo=timezone.utc).isoformat(),
                "url": "https://twitter.com/karachi_updates/status/12345"
            },
            {
                "id": "a11b2203-b3c4-44d3-8415-53188a8cf1f1",
                "title": "National Grid Failure Causes Major Power Outage in Islamabad and Rawalpindi",
                "body": "A technical fault in the national grid has caused a complete power outage affecting Islamabad, Rawalpindi, and surrounding areas. The power outage has affected main hospitals and internet services, leading to infrastructure failure. Teams are working on system recovery.",
                "source": "facebook",
                "category": "Infrastructure Failure",
                "confidence": 0.94,
                "region": "Islamabad",
                "timestamp": datetime(2026, 5, 17, 8, 45, 0, tzinfo=timezone.utc).isoformat(),
                "url": "https://facebook.com/dawn/posts/98765"
            },
            {
                "id": "b22c3304-c4d5-55e4-9526-64299b9df2g2",
                "title": "Dengue Prevention and Safety Tips: How to Protect Your Family",
                "body": "As dengue cases rise across Pakistan, healthcare professionals have released critical guidelines and safety tips. To prevent mosquito bites, use repellents, wear long sleeves, and sanitize your surroundings. Regular hygiene is highly encouraged.",
                "source": "news",
                "category": "Prevention/Safety Tips",
                "confidence": 0.81,
                "region": "Punjab",
                "timestamp": datetime(2026, 5, 16, 10, 0, 0, tzinfo=timezone.utc).isoformat(),
                "url": "https://dawn.com/news/dengue-prevention-tips"
            },
            {
                "id": "c33d4405-d5e6-66f5-9537-75399c9ef3h3",
                "title": "Critical Shortage of Essential Dengue Medications Spikes Drug Prices in Pharmacies",
                "body": "A severe shortage of paracetamol and other essential medications has been reported in local pharmacies in Lahore and Rawalpindi. Drug prices have spiked by 50% due to supply chain issues. Pharmaceutical authorities are monitoring the stock updates.",
                "source": "news",
                "category": "Pharmacy/Cost Updates",
                "confidence": 0.89,
                "region": "Lahore",
                "timestamp": datetime(2026, 5, 15, 14, 15, 0, tzinfo=timezone.utc).isoformat(),
                "url": "https://thenews.com.pk/pharmacy-cost-dengue"
            }
        ]
        
        col = self.collection("articles")
        for art in articles:
            col.document(art["id"]).set(art)

_real_client = None
_mock_client = None

def get_firestore_client():
    """Get the active Firestore client.
    
    Returns the real google-cloud-firestore Client if credentials exist,
    otherwise falls back gracefully to a fully functional in-memory MockFirestoreClient.
    """
    global _real_client, _mock_client
    
    # Check if mock client is explicitly forced
    if os.getenv("USE_MOCK_FIRESTORE", "false").lower() == "true":
        if _mock_client is None:
            _mock_client = MockFirestoreClient()
            logger.info("using_mock_firestore_forced")
        return _mock_client

    try:
        from google.cloud import firestore
        if _real_client is None:
            if settings.google_cloud_project:
                _real_client = firestore.Client(project=settings.google_cloud_project)
            else:
                _real_client = firestore.Client()
            logger.info("using_real_firestore")
        return _real_client
    except Exception as e:
        if _mock_client is None:
            _mock_client = MockFirestoreClient()
            logger.info("using_mock_firestore_fallback", reason=str(e))
        return _mock_client
