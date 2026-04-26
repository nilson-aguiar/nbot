# CI/CD and Renovate Setup Design

**Goal:** Implement automated dependency management with Renovate and a robust CI/CD pipeline using GitHub Actions, following the patterns in the `manga-fetcher` repository.

## Architecture

1.  **Dependency Management**: 
    - A `renovate.json` file in the root directory to configure the Renovate bot.
    - It will extend recommended presets and group non-major updates to reduce PR noise.

2.  **Continuous Integration / Continuous Deployment (CI/CD)**:
    - **Trigger**: Pushes to `main`, pull requests, and releases.
    - **Release Management**: `release-please-action` to handle automated versioning based on conventional commits.
    - **Build Job**: 
        - JDK 21 setup (matching `build.gradle.kts`).
        - Gradle build and test.
        - Docker build and push to GHCR (GitHub Container Registry).
        - Multi-platform support (`linux/amd64`, `linux/arm64`) using Buildx.
        - Strategic tagging: `latest` for releases, `unstable` for `main` branch, and semver tags.

## Components

### 1. `renovate.json`
- Extends: `config:recommended`, `group:allNonMajor`.
- Labels: `dependencies`.
- Dashboard enabled.

### 2. `.github/workflows/build.yml`
- **Concurrency**: Grouped by workflow and ref to cancel in-progress runs.
- **Jobs**:
    - `release`: Runs `release-please-action`.
    - `build`: 
        - Depends on `release`.
        - Steps: Checkout, Setup Java (JDK 21), Gradle Check, QEMU/Buildx setup, GHCR Login, Docker Metadata, Docker Build/Push.

## Data Flow
1. Developer pushes code to `main`.
2. `release-please` checks for conventional commits and opens/updates a Release PR.
3. If a release is created, the `build` job triggers, builds the JAR, builds the Docker image, and tags it with the new version and `latest`.
4. On standard pushes to `main`, the image is tagged as `unstable`.

## Testing
- The CI pipeline itself tests the code via `./gradlew check`.
- Docker build step verifies containerization.
- PRs will trigger the build job (without pushing) to verify changes.
