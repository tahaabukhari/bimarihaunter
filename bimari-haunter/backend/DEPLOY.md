Deployment to Google Cloud Run

Overview
- Service name: bimari_haunter
- Uses the Dockerfile in the repository (listening on port 8080 via Uvicorn)

Quick local deploy (PowerShell)

1. Authenticate and set project:

```powershell
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

2. Run the helper script (from backend directory). The script defaults to the Google Cloud South-East region `asia-southeast1`.

Example (deploy to Singapore / South-East region):

```powershell
.\scripts\deploy_cloud_run.ps1 -PROJECT_ID YOUR_PROJECT_ID -REGION asia-southeast1 -IMAGE_TAG latest
```

If you prefer another region, pass its region code (for example `australia-southeast1` for Sydney).

What the script does
- Enables Cloud Run + Cloud Build APIs
- Submits a Cloud Build that builds and pushes image to `gcr.io/$PROJECT_ID/bimari_haunter:IMAGE_TAG`
- Deploys to Cloud Run as service `bimari_haunter`

Permissions required
- The user/service account should have: `roles/run.admin`, `roles/cloudbuild.builds.editor`, `roles/storage.admin` (for pushing image to GCR) or equivalent.

Optional: GitHub Actions / CI
- You can use `cloudbuild.yaml` at the repo root to trigger Cloud Build on push, or create a GitHub Actions workflow to build and deploy.

Environment variables & secrets
- If your app depends on secrets, use `Secret Manager` and set them via `--set-secrets` in `gcloud run deploy` or add runtime variables in the Cloud Run console.

Notes
- The Dockerfile installs heavy models (spaCy, HF models) during build; this will increase build time and image size. Consider caching strategy or building models at runtime if needed.
- If you prefer Artifact Registry instead of gcr.io, update `cloudbuild.yaml` and `deploy_cloud_run.ps1` accordingly.
