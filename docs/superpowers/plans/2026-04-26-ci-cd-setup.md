# CI/CD and Renovate Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement automated dependency updates with Renovate and a GitHub Actions workflow for building and pushing Docker images to GHCR.

**Architecture:** A `renovate.json` file for dependency management and a `.github/workflows/build.yml` workflow using `release-please-action` for versioning and `build-push-action` for multi-platform Docker builds.

**Tech Stack:** Renovate, GitHub Actions, Docker, Gradle, JDK 21.

---

### Task 1: Add Renovate Configuration

**Files:**
- Create: `renovate.json`

- [ ] **Step 1: Create renovate.json**

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": [
    "config:recommended",
    "group:allNonMajor"
  ],
  "dependencyDashboard": true,
  "labels": ["dependencies"]
}
```

- [ ] **Step 2: Commit**

```bash
git add renovate.json
git commit -m "chore: add renovate configuration"
```

### Task 2: Add GitHub Actions Workflow

**Files:**
- Create: `.github/workflows/build.yml`

- [ ] **Step 1: Create .github/workflows/build.yml**

```yaml
name: Build and Push

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  release:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      pull-requests: write
    outputs:
      release_created: ${{ steps.release.outputs.release_created }}
      tag_name: ${{ steps.release.outputs.tag_name }}
    steps:
      - uses: googleapis/release-please-action@v4
        id: release
        with:
          release-type: simple

  build:
    needs: [release]
    if: always() && !cancelled() && !failure()
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: 'gradle'

    - name: Build and Test with Gradle
      run: ./gradlew build

    - name: Set up QEMU
      uses: docker/setup-qemu-action@v3

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3

    - name: Log in to GHCR
      if: github.event_name != 'pull_request'
      uses: docker/login-action@v3
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Docker Metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ghcr.io/${{ github.repository }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          # Semantic Versioning (Releases)
          type=semver,pattern={{version}},value=${{ needs.release.outputs.tag_name }},enable=${{ needs.release.outputs.release_created == 'true' }}
          type=semver,pattern={{major}}.{{minor}},value=${{ needs.release.outputs.tag_name }},enable=${{ needs.release.outputs.release_created == 'true' }}
          type=semver,pattern={{major}},value=${{ needs.release.outputs.tag_name }},enable=${{ needs.release.outputs.release_created == 'true' }}
          type=raw,value=latest,enable=${{ needs.release.outputs.release_created == 'true' }}
          # Unstable / Development Tags
          type=raw,value=unstable,enable=${{ github.ref == 'refs/heads/main' }}
          type=sha,format=short

    - name: Build and Push Docker Image
      uses: docker/build-push-action@v5
      with:
        context: .
        file: Dockerfile
        platforms: linux/amd64,linux/arm64
        push: ${{ github.event_name != 'pull_request' }}
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add github actions workflow for build and push"
```

### Task 3: Verification

- [ ] **Step 1: Verify renovate.json locally**
Check if JSON is valid: `jq . renovate.json`

- [ ] **Step 2: Dry run build (if possible)**
Run `./gradlew build` to ensure local build passes before pushing CI config.

- [ ] **Step 3: Commit all remaining changes**
```bash
git status
# Ensure everything is clean
```
