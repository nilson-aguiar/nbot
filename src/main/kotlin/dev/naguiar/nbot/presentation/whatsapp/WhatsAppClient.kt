package dev.naguiar.nbot.presentation.whatsapp

import dev.naguiar.nbot.infrastructure.config.WhatsAppProperties
import java.io.InputStream
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class WhatsAppClient(
    private val properties: WhatsAppProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val log = LoggerFactory.getLogger(WhatsAppClient::class.java)

    private val restClient =
        restClientBuilder
            .baseUrl("https://graph.facebook.com/v20.0")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.apiToken}")
            .build()

    fun sendTextMessage(
        to: String,
        text: String,
    ) {
        if (!properties.enabled) return
        try {
            val body =
                mapOf(
                    "messaging_product" to "whatsapp",
                    "recipient_type" to "individual",
                    "to" to to,
                    "type" to "text",
                    "text" to mapOf("body" to text),
                )
            restClient
                .post()
                .uri("/{phoneNumberId}/messages", properties.phoneNumberId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
            log.info("Successfully sent WhatsApp message to {}", to)
        } catch (e: Exception) {
            log.error("Failed to send WhatsApp message to $to", e)
        }
    }

    private data class MediaResponse(
        val url: String = "",
        val mime_type: String? = null,
        val sha256: String? = null,
        val file_size: Long? = null,
        val id: String? = null,
        val messaging_product: String? = null,
    )

    fun downloadMedia(mediaId: String): InputStream? {
        if (!properties.enabled) return null
        return try {
            log.info("Fetching metadata for media ID: {}", mediaId)
            val mediaResponse =
                restClient
                    .get()
                    .uri("/{mediaId}", mediaId)
                    .retrieve()
                    .body(MediaResponse::class.java)

            val downloadUrl = mediaResponse?.url ?: return null
            log.info("Downloading media from: {}", downloadUrl)

            val resource =
                restClient
                    .get()
                    .uri(downloadUrl)
                    .retrieve()
                    .body(Resource::class.java)

            resource?.inputStream
        } catch (e: Exception) {
            log.error("Failed to download media for ID $mediaId", e)
            null
        }
    }
}
