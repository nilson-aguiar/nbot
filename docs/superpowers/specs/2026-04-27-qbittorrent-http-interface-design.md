# Design Spec: Convert QBittorrentClient to Spring HTTP Interfaces

## Context
The current `QBittorrentClient` uses manual `RestClient` calls. This spec outlines the migration to Spring's declarative HTTP interfaces (`@HttpExchange`) to improve readability and maintainability.

## Proposed Changes

### 1. New Interface: `QBittorrentApi`
Define a declarative interface for the qBittorrent API v2.

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

### 2. Refactor `QBittorrentClient`
Update `QBittorrentClient` to use the new interface instead of building `RestClient` manually.

- Maintain `authCookie` state.
- Keep retry logic for authentication failures.
- Delegate HTTP calls to `QBittorrentApi`.

### 3. Infrastructure Configuration
Add a configuration bean to instantiate the proxy for `QBittorrentApi`.

```kotlin
@Configuration
class QBittorrentConfig {
    @Bean
    fun qBittorrentApi(builder: RestClient.Builder, properties: QBittorrentProperties): QBittorrentApi {
        val client = builder.baseUrl(properties.url).build()
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(client))
            .build()
            .createClient(QBittorrentApi::class.java)
    }
}
```

## Testing Strategy
- **Unit Tests:** Update `QBittorrentClientTest` to mock `QBittorrentApi`.
- **Integration Tests:** Run existing `QBittorrentClientIT` to ensure connectivity with a real/testcontainers instance still works.

## Success Criteria
- `QBittorrentClient` no longer contains low-level HTTP call logic.
- All existing tests pass.
- Application context starts successfully with the new proxy bean.
