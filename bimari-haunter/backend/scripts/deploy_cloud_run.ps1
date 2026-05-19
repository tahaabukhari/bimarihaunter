param(
  [string]$PROJECT_ID = $(Read-Host "GCP Project ID"),
  [string]$REGION = "asia-southeast1",
  [string]$IMAGE_TAG = "latest"
)

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
  Write-Error "gcloud CLI not found. Install and authenticate first: https://cloud.google.com/sdk/docs/install"
  exit 1
}

Write-Host "Enabling required APIs..."
& gcloud services enable run.googleapis.com cloudbuild.googleapis.com --project $PROJECT_ID

$fullImage = "gcr.io/$PROJECT_ID/bimari-haunter:$IMAGE_TAG"

Write-Host "Building and submitting Cloud Build..."
& gcloud builds submit --tag $fullImage --project $PROJECT_ID

Write-Host "Deploying to Cloud Run as service 'bimari-haunter'..."
& gcloud run deploy bimari-haunter --image $fullImage --region $REGION --platform managed --allow-unauthenticated --memory 4Gi --cpu 2 --project $PROJECT_ID

Write-Host "Done. Visit the service URL shown above."