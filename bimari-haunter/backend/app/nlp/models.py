"""
NLP model loader.

Lazy-loads heavy ML models so they are only initialised once per
process.  Import the singletons from here rather than creating new
pipeline instances elsewhere.
"""

from __future__ import annotations

import functools

import structlog

logger = structlog.get_logger(__name__)


@functools.lru_cache(maxsize=1)
def load_spacy_model():
    """Load the spaCy transformer NER model."""
    import spacy

    logger.info("loading_spacy_model", model="en_core_web_trf")
    nlp = spacy.load("en_core_web_trf")
    logger.info("spacy_model_loaded")
    return nlp


@functools.lru_cache(maxsize=1)
def load_zero_shot_classifier():
    """Load a classifier pipeline.

    Prefer a local, trained classifier (added under `/classifier`) if available;
    otherwise fall back to the HuggingFace zero-shot pipeline.
    """
    # Try to use the local trained classifier package first (if installed/available
    # on PYTHONPATH as the sibling `classifier` package inside the repo).
    try:
        from classifier.app.classifier import classify_text as _local_classify

        logger.info("using_local_classifier")

        class _LocalWrapper:
            def __call__(self, text, candidate_labels=None, **kwargs):
                res = _local_classify(text)
                pred_label = res.get("category")
                confidence = float(res.get("confidence", 0.0))

                # If candidate_labels provided, build a full labels/scores list
                if candidate_labels:
                    labels = list(candidate_labels)
                    scores = [round(confidence, 4) if lab == pred_label else 0.0 for lab in labels]

                    # Ensure predicted label appears first (mimic HF output ordering)
                    if labels[0] != pred_label:
                        try:
                            idx = labels.index(pred_label)
                            labels.insert(0, labels.pop(idx))
                            scores.insert(0, scores.pop(idx))
                        except ValueError:
                            labels.insert(0, pred_label)
                            scores.insert(0, round(confidence, 4))
                else:
                    labels = [pred_label]
                    scores = [round(confidence, 4)]

                return {"labels": labels, "scores": scores}

        return _LocalWrapper()
    except Exception:
        # Fall back to HuggingFace pipeline if local classifier is not available
        from transformers import pipeline

        logger.info("loading_classifier", model="facebook/bart-large-mnli")
        classifier = pipeline(
            "zero-shot-classification",
            model="facebook/bart-large-mnli",
        )
        logger.info("classifier_loaded")
        return classifier


@functools.lru_cache(maxsize=1)
def load_summarizer():
    """Load the HuggingFace summarization pipeline."""
    from transformers import pipeline

    logger.info("loading_summarizer", model="facebook/bart-large-cnn")
    summarizer = pipeline(
        "summarization",
        model="facebook/bart-large-cnn",
    )
    logger.info("summarizer_loaded")
    return summarizer
