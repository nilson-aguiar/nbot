# QBittorrent HTTP Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert `QBittorrentClient` to use Spring Boot 3.2+ declarative HTTP interfaces (`@HttpExchange`) for cleaner code and better abstraction.

**Architecture:** 
- Define `QBittorrentApi` interface with `@PostExchange` annotations.
- Create `QBittorrentConfig` to instantiate the proxy via `HttpServiceProxyFactory`.
- Refactor `QBittorrentClient` to delegate HTTP calls to the new interface.
- Maintain existing stateful authentication (cookie management) in `QBittorrentClient`.

**Tech Stack:** Kotlin, Spring Boot 3.5, Spring Framework 6.2 (HTTP Interfaces), Mockk.

---

### Task 1: Create the Declarative Interface

**Files:**
- Create: `src/main/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentApi.kt`

- [ ] **Step 1: Create QBittorrentApi interface**

```kotlin
package dev.naguiar.nbot.tools.torrent

import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.service.annotation.PostExchange

interface QBittorrentApi {
    @PostExchange("/api/v2/auth/login", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    fun login(@RequestBody body: MultiValueMap<String, String>): ResponseEntity<Void>

    @PostExchange("/api/v2/torrents/add", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    fun addMagnetLink(
        @RequestHeader(HttpHeaders.COOKIE) cookie: String,
        @RequestParam("urls") urls: String
    ): ResponseEntity<Void>

    @PostExchange("/api/v2/torrents/add", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    fun addTorrentFile(
        @RequestHeader(HttpHeaders.COOKIE) cookie: String,
        @RequestPart("torrents") file: Resource
    ): ResponseEntity<Void>
}
```

- [ ] **Step 2: Commit changes**

```bash
git add src/main/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentApi.kt
git commit -m "feat(torrent): add QBittorrentApi interface for declarative HTTP calls"
```

---

### Task 2: Infrastructure Configuration

**Files:**
- Create: `src/main/kotlin/dev/naguiar/nbot/infrastructure/config/QBittorrentConfig.kt`

- [ ] **Step 1: Create QBittorrentConfig class**

```kotlin
package dev.naguiar.nbot.infrastructure.config

import dev.naguiar.nbot.tools.torrent.QBittorrentApi
import dev.naguiar.nbot.tools.torrent.QBittorrentProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class QBittorrentConfig {
    @Bean
    fun qBittorrentApi(builder: RestClient.Builder, properties: QBittorrentProperties): QBittorrentApi {
        val client = builder.baseUrl(properties.url).build()
        val adapter = RestClientAdapter.create(client)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(QBittorrentApi::class.java)
    }
}
```

- [ ] **Step 2: Commit changes**

```bash
git add src/main/kotlin/dev/naguiar/nbot/infrastructure/config/QBittorrentConfig.kt
git commit -m "feat(config): add QBittorrentConfig to instantiate QBittorrentApi proxy"
```

---

### Task 3: Refactor QBittorrentClient

**Files:**
- Modify: `src/main/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClient.kt`

- [ ] **Step 1: Update QBittorrentClient to use QBittorrentApi**

```kotlin
package dev.naguiar.nbot.tools.torrent

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import java.io.File

@Service
class QBittorrentClient(
    private val api: QBittorrentApi,
    private val properties: QBittorrentProperties,
) {
    private val log = LoggerFactory.getLogger(QBittorrentClient::class.java)
    private var authCookie: String? = null

    private fun login() {
        log.info("Logging into qBittorrent at {}", properties.url)

        val body = LinkedMultiValueMap<String, String>().apply {
            add("username", properties.username)
            add("password", properties.password)
        }

        try {
            val response = api.login(body)
            val cookies = response.headers[HttpHeaders.SET_COOKIE]
            if (!cookies.isNullOrEmpty()) {
                authCookie = cookies.find { it.startsWith("SID=") }?.substringBefore(";")
                log.info("Successfully logged in.")
            } else {
                log.error("Login failed, no cookie received.")
            }
        } catch (e: Exception) {
            log.error("Login failed with error", e)
        }
    }

    private fun <T> executeWithRetry(action: (String) -> Boolean): Boolean {
        if (authCookie == null) login()
        val cookie = authCookie ?: return false

        return try {
            action(cookie)
        } catch (e: Exception) {
            log.warn("Request failed, attempting re-login", e)
            login()
            val newCookie = authCookie ?: return false
            try {
                action(newCookie)
            } catch (retryException: Exception) {
                log.error("Request failed after retry", retryException)
                false
            }
        }
    }

    fun addMagnetLink(urls: String): Boolean {
        return executeWithRetry { cookie ->
            val response = api.addMagnetLink(cookie, urls)
            response.statusCode.is2xxSuccessful
        }
    }

    fun addTorrentFile(file: File): Boolean {
        return executeWithRetry { cookie ->
            val resource = FileSystemResource(file)
            val response = api.addTorrentFile(cookie, resource)
            response.statusCode.is2xxSuccessful
        }
    }
}
```

- [ ] **Step 2: Commit changes**

```bash
git add src/main/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClient.kt
git commit -m "refactor(torrent): use QBittorrentApi in QBittorrentClient"
```

---

### Task 4: Verify with Tests

**Files:**
- Modify: `src/test/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClientTest.kt`

- [ ] **Step 1: Update QBittorrentClientTest configuration**
We need to import `QBittorrentConfig` so the `QBittorrentApi` bean is available.

```kotlin
// ... existing imports ...
import dev.naguiar.nbot.infrastructure.config.QBittorrentConfig
import org.springframework.context.annotation.Import

@RestClientTest(QBittorrentClient::class)
@Import(QBittorrentConfig::class) // Add this
@EnableConfigurationProperties(QBittorrentProperties::class)
// ... rest of class ...
```

- [ ] **Step 2: Run tests to verify everything passes**

Run: `./gradlew test --tests dev.naguiar.nbot.tools.torrent.QBittorrentClientTest`
Expected: ALL PASS

- [ ] **Step 3: Run integration tests**

Run: `./gradlew test --tests dev.naguiar.nbot.tools.torrent.QBittorrentClientIT`
Expected: ALL PASS (if a local qBittorrent is available or using testcontainers)

- [ ] **Step 4: Commit test changes**

```bash
git add src/test/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClientTest.kt
git commit -m "test(torrent): update QBittorrentClientTest to work with HTTP interface"
```
