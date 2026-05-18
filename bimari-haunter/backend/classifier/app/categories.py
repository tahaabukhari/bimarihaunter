
# ============================================================
# BimariHunter - Crisis Category Definitions
# ============================================================
# Each category maps to a list of relevant keywords used
# by the keyword_filter module for fast rule-based matching.
# ============================================================

CATEGORIES = {
    "Disease Outbreak": [
        "disease", "outbreak", "epidemic", "pandemic",
        "virus", "infection", "quarantine", "vaccine",
        "contagious", "pathogen", "symptoms", "fever",
        "transmission", "immunization", "mortality"
    ],

    "Natural Disaster": [
        "flood", "earthquake", "wildfire",
        "storm", "cyclone", "tsunami",
        "hurricane", "tornado", "drought",
        "avalanche", "landslide", "volcanic"
    ],

    "Infrastructure Failure": [
        "power outage", "bridge collapse",
        "internet outage", "water supply",
        "blackout", "grid failure", "pipeline",
        "system failure", "network down", "sewage"
    ],

    "Accidents/Emergencies": [
        "accident", "explosion", "fire",
        "collision", "chemical spill",
        "crash", "blast", "emergency",
        "hazardous", "toxic leak", "evacuation"
    ],

    "Community Response/Aid": [
        "volunteers", "donations",
        "relief efforts", "shelter",
        "rescue", "aid workers", "charity",
        "fundraising", "support center", "humanitarian"
    ],

    "Prevention/Safety Tips": [
        "prevention", "guidelines",
        "precautions", "first aid",
        "safety tips", "awareness", "hygiene",
        "protective gear", "mask", "sanitize",
        "social distancing", "lockdown advice"
    ],

    "Pharmacy/Cost Updates": [
        "drug prices", "medication",
        "shortage", "pharmacy",
        "medicine cost", "stock", "supply chain",
        "pharmaceutical", "prescription", "healthcare cost"
    ],

    "General News": [
        "government announcement",
        "community event",
        "press release", "official statement",
        "policy update", "public notice"
    ]
}
