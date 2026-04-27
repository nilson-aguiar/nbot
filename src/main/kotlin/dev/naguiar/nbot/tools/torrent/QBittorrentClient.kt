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

    private fun executeWithRetry(action: (String) -> Boolean): Boolean {
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
