package dev.naguiar.nbot.tools.torrent

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange

interface QBittorrentApi {
    @PostExchange("/api/v2/auth/login", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    fun login(
        @RequestBody body: MultiValueMap<String, String>,
    ): ResponseEntity<Void>

    @PostExchange("/api/v2/torrents/add", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    fun addMagnetLink(
        @RequestHeader(HttpHeaders.COOKIE) cookie: String,
        @RequestBody body: MultiValueMap<String, String>,
    ): ResponseEntity<Void>

    @PostExchange("/api/v2/torrents/add", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    fun addTorrentFile(
        @RequestHeader(HttpHeaders.COOKIE) cookie: String,
        @RequestBody body: MultiValueMap<String, Any>,
    ): ResponseEntity<Void>

    @GetExchange("/api/v2/torrents/info")
    fun getTorrentsInfo(
        @RequestHeader(HttpHeaders.COOKIE) cookie: String,
    ): ResponseEntity<List<Map<String, Any>>>
}
