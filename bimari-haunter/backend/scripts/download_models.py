import sys
import time

def download_with_retry():
    try:
        from transformers import pipeline
        
        print("Starting download of 'facebook/bart-large-mnli'...")
        pipeline('zero-shot-classification', model='facebook/bart-large-mnli')
        print("Successfully downloaded 'facebook/bart-large-mnli'")
        
        print("Starting download of 'facebook/bart-large-cnn'...")
        pipeline('summarization', model='facebook/bart-large-cnn')
        print("Successfully downloaded 'facebook/bart-large-cnn'")
        
        print("Models cached successfully")
    except Exception as e:
        print(f"Error downloading models: {e}")
        sys.exit(1)

if __name__ == "__main__":
    download_with_retry()
