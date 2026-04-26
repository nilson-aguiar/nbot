# Code Coverage Setup Design

**Goal:** Integrate JaCoCo for code coverage reporting and display the results in GitHub Pull Requests.

## Architecture

1.  **Coverage Engine**: 
    - Use JaCoCo (Java Code Coverage library) via the Gradle plugin.
    - Configure it to generate XML reports for integration with GitHub Actions.
    - Set a minimum coverage threshold of 50%.

2.  **GitHub Integration**:
    - **Trigger**: Pull Request events.
    - **Action**: Use `madrapps/jacoco-report` to parse the XML report and post a comment on the PR.
    - **Threshold Enforcement**: Configure the build to fail if the overall coverage is below 50%.

## Components

### 1. `build.gradle.kts`
- Add `jacoco` plugin.
- Configure `jacocoTestReport` task to output XML.
- Configure `jacocoTestCoverageVerification` task with a 50% limit.

### 2. `.github/workflows/build.yml`
- Add a step to run `./gradlew jacocoTestReport jacocoTestCoverageVerification`.
- Add a step to post the PR comment using `madrapps/jacoco-report`.

## Data Flow
1. Developer pushes code to a branch and opens a PR.
2. GitHub Actions triggers the `build` workflow.
3. Gradle runs tests, then JaCoCo generates the coverage report and verifies the threshold.
4. If successful, the coverage report action reads the XML and updates the PR comment.

## Testing
- Verify that `./gradlew jacocoTestReport` generates a report in `build/reports/jacoco/test/jacocoTestReport.xml`.
- Verify that `./gradlew jacocoTestCoverageVerification` fails if coverage is below 50%.
