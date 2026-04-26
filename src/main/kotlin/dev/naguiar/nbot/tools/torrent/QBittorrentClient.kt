package dev.naguiar.nbot.tools.torrent

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient

@Service
class QBittorrentClient(
    private val properties: QBittorrentProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val log = LoggerFactory.getLogger(QBittorrentClient::class.java)
    private val restClient = restClientBuilder.baseUrl(properties.url).build()
    private var authCookie: String? = null

    private fun login() {
        log.info("Logging into qBittorrent at {}", properties.url)
        val body =
            LinkedMultiValueMap<String, String>().apply {
                add("username", properties.username)
                add("password", properties.password)
            }
        val response =
            restClient
                .post()
                .uri("/api/v2/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity()

        val cookies = response.headers[HttpHeaders.SET_COOKIE]
        if (!cookies.isNullOrEmpty()) {
            authCookie = cookies.find { it.startsWith("SID=") }?.substringBefore(";")
            log.info("Successfully logged in.")
        } else {
            log.error("Login failed, no cookie received.")
        }
    }

    private fun sendTorrentAddRequest(body: MultiValueMap<String, Any>, contentType: MediaType): Boolean {
        if (authCookie == null) login()

        return try {
            val response =
                restClient
                    .post()
                    .uri("/api/v2/torrents/add")
                    .header(HttpHeaders.COOKIE, authCookie)
                    .contentType(contentType)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity()

            response.statusCode.is2xxSuccessful
        } catch (e: Exception) {
            log.warn("Failed to add torrent, attempting re-login", e)
            try {
                login()
                val retryResponse =
                    restClient
                        .post()
                        .uri("/api/v2/torrents/add")
                        .header(HttpHeaders.COOKIE, authCookie)
                        .contentType(contentType)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity()
                retryResponse.statusCode.is2xxSuccessful
            } catch (retryException: Exception) {
                log.error("Failed to add torrent after retry", retryException)
                false
            }
        }
    }

    fun addMagnetLink(urls: String): Boolean {
        val body =
            LinkedMultiValueMap<String, Any>().apply {
                add("urls", urls)
            }
        return sendTorrentAddRequest(body, MediaType.APPLICATION_FORM_URLENCODED)
    }

    fun addTorrentFile(file: java.io.File): Boolean {
        val body =
            LinkedMultiValueMap<String, Any>().apply {
                add("torrents", org.springframework.core.io.FileSystemResource(file))
            }
        return sendTorrentAddRequest(body, MediaType.MULTIPART_FORM_DATA)
    }
}
