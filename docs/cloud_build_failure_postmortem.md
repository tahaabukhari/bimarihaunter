# Cloud Build Failure: Playwright Dependencies

## The Error
During the Docker build on Google Cloud, the process failed at step 6: `RUN playwright install chromium --with-deps`.

The build logs output the following:
```
BEWARE: your OS is not officially supported by Playwright; installing dependencies for ubuntu20.04-x64 as a fallback.
...
Package ttf-unifont is not available...
E: Package 'ttf-unifont' has no installation candidate
E: Package 'ttf-ubuntu-font-family' has no installation candidate
Failed to install browsers
Error: Installation process exited with code: 100
```

## Root Cause
The `Dockerfile` was using `python:3.11-slim` as the base image. This image is based on Debian (Trixie/Bookworm). Playwright's automated dependency installer (`--with-deps`) is officially optimized for Ubuntu (like 20.04 Focal or 22.04 Jammy). When Playwright attempts to install Ubuntu-specific font packages (`ttf-ubuntu-font-family`) on Debian, the package manager (`apt-get`) fails to find them and crashes the build.

## The Fix
To guarantee Playwright has all the system dependencies it needs without fighting the OS package manager, we must change the base Docker image to Microsoft's official Playwright Python image. 

I updated the `Dockerfile` to use:
`FROM mcr.microsoft.com/playwright/python:v1.41.0-jammy`

This image is based on Ubuntu 22.04 (Jammy) and already comes pre-installed with Python, Chromium, and all system-level dependencies required for web scraping. This allows us to remove the brittle `apt-get` and `playwright install --with-deps` lines entirely from our `Dockerfile`, drastically speeding up the build and ensuring stability.
