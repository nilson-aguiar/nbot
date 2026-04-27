# Fix Spring Boot and Spring Cloud Version Mismatch

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `NoSuchMethodError` in tests caused by incompatible Spring Cloud version.

**Architecture:** Downgrade Spring Cloud version to align with Spring Boot 3.5.x.

**Tech Stack:** Gradle, Spring Boot, Spring Cloud.

---

### Task 1: Downgrade Spring Cloud Version

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Update springCloudVersion**

Change `extra["springCloudVersion"] = "2025.1.1"` to `extra["springCloudVersion"] = "2025.0.0"`.

- [ ] **Step 2: Run tests to verify the fix**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "fix: downgrade springCloudVersion to 2025.0.0 for compatibility with Spring Boot 3.5.x"
```
