# Plan: Convert Configuration Properties to Data Classes

We will convert `SecurityProperties` and `QBittorrentProperties` from mutable classes to immutable Kotlin `data class`es with constructor binding. This is more idiomatic in Kotlin/Spring Boot 3.x and leverages `@ConfigurationPropertiesScan`. We will also fix the `QBittorrentClientTest` which currently relies on mutability.

## Proposed Changes

### 1. Convert `SecurityProperties` to `data class`
- **File:** `src/main/kotlin/dev/naguiar/nbot/infrastructure/config/SecurityProperties.kt`
- **Changes:**
    - Convert to `data class`.
    - Change properties to `val`.
    - Remove `@Configuration` annotation.
    - `allowedUsers` should have `emptyList()` as default.
    - `telegramBotToken` should have **no default value**.

### 2. Convert `QBittorrentProperties` to `data class`
- **File:** `src/main/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentProperties.kt`
- **Changes:**
    - Convert to `data class`.
    - Change properties to `val`.
    - Remove `@Configuration` annotation.
    - **No default values** should be provided in the constructor.

### 3. Fix `QBittorrentClientTest`
- **File:** `src/test/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClientTest.kt`
- **Changes:**
    - Since properties are now immutable, use `@TestPropertySource` to define properties for the tests.
    - Remove manual property assignments in `@Test` methods.

## Verification Plan

### Automated Tests
- Run `./gradlew test` to ensure all tests pass.
- Specifically verify `QBittorrentClientTest` passes.
