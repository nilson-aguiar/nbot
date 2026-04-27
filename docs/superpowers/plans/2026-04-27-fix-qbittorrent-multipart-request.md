# Fix QBittorrent Multipart Request Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align `QBittorrentApi` with official documentation by using `multipart/form-data` for magnet links and ensuring parameters are sent as request parts.

**Architecture:** 
- Modify `QBittorrentApi` to use `@RequestPart` for `urls`.
- Change `addMagnetLink` content type to `multipart/form-data`.
- Update `QBittorrentClientTest` to verify `multipart/form-data` is used for magnet links.

**Tech Stack:** Kotlin, Spring Boot 3.5.

---

### Task 1: Update QBittorrentApi Interface

**Files:**
- Modify: `src/main/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentApi.kt`

- [ ] **Step 1: Update addMagnetLink to use multipart/form-data and @RequestPart**

```kotlin
package dev.naguiar.nbot.tools.torrent

import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.service.annotation.PostExchange

interface QBittorrentApi {
    @PostExchange("/api/v2/auth/login", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    fun login(@RequestBody body: MultiValueMap<String, String>): ResponseEntity<Void>

    @PostExchange("/api/v2/torrents/add", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    fun addMagnetLink(
        @RequestHeader(HttpHeaders.COOKIE) cookie: String,
        @RequestPart("urls") urls: String
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
git commit -m "fix(torrent): use multipart/form-data for magnet links in QBittorrentApi"
```

---

### Task 2: Verify with Tests

**Files:**
- Modify: `src/test/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClientTest.kt`

- [ ] **Step 1: Update QBittorrentClientTest to expect multipart/form-data for magnet links**

```kotlin
    @Test
    fun `addMagnetLink should login and return true on success`() {
        // Mock login
        server
            .expect(requestTo("http://localhost:8080/api/v2/auth/login"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andRespond(withSuccess().header(HttpHeaders.SET_COOKIE, "SID=12345; HttpOnly"))

        // Mock add torrent
        server
            .expect(requestTo("http://localhost:8080/api/v2/torrents/add"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.COOKIE, "SID=12345"))
            // CHANGED: Expect MULTIPART_FORM_DATA instead of APPLICATION_FORM_URLENCODED
            .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
            .andRespond(withSuccess())

        val result = client.addMagnetLink("magnet:?xt=urn:btih:fake")

        assertThat(result).isTrue()
        server.verify()
    }
```

- [ ] **Step 2: Run tests to verify everything passes**

Run: `./gradlew test --tests dev.naguiar.nbot.tools.torrent.QBittorrentClientTest`
Expected: ALL PASS

- [ ] **Step 3: Run integration tests**

Run: `./gradlew test --tests dev.naguiar.nbot.tools.torrent.QBittorrentClientIT`
Expected: ALL PASS

- [ ] **Step 4: Commit test changes**

```bash
git add src/test/kotlin/dev/naguiar/nbot/tools/torrent/QBittorrentClientTest.kt
git commit -m "test(torrent): update tests to verify multipart/form-data for magnet links"
```
