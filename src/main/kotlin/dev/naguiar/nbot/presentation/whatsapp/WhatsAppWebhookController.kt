package dev.naguiar.nbot.presentation.whatsapp

import com.fasterxml.jackson.annotation.JsonProperty
import dev.naguiar.nbot.application.BotMessageDispatcher
import dev.naguiar.nbot.infrastructure.config.WhatsAppProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhooks/whatsapp")
class WhatsAppWebhookController(
    private val properties: WhatsAppProperties,
    private val whatsAppClient: WhatsAppClient,
    private val botMessageDispatcher: BotMessageDispatcher,
) {
    private val log = LoggerFactory.getLogger(WhatsAppWebhookController::class.java)

    @GetMapping
    fun verifyWebhook(
        @RequestParam("hub.mode") mode: String,
        @RequestParam("hub.verify_token") token: String,
        @RequestParam("hub.challenge") challenge: String,
    ): ResponseEntity<String> {
        log.info("Received WhatsApp webhook verification request with mode: {}", mode)
        return if (mode == "subscribe" && token == properties.verifyToken) {
            log.info("Webhook verified successfully.")
            ResponseEntity.ok(challenge)
        } else {
            log.warn("Webhook verification failed. Mode: {}, Token: {}", mode, token)
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @PostMapping
    fun receiveWebhook(
        @RequestBody payload: WhatsAppWebhookPayload,
    ): ResponseEntity<Void> {
        if (!properties.enabled) {
            return ResponseEntity.ok().build()
        }

        val entries = payload.entry ?: return ResponseEntity.ok().build()
        for (entry in entries) {
            val changes = entry.changes ?: continue
            for (change in changes) {
                val value = change.value ?: continue
                val messages = value.messages ?: continue
                for (message in messages) {
                    val from = message.from ?: continue

                    if (!properties.allowedNumbers.contains(from)) {
                        log.warn("Unauthorized contact attempt from WhatsApp number: {}", from)
                        continue
                    }

                    when (message.type) {
                        "text" -> {
                            handleTextMessage(from, message.text?.body)
                        }

                        "document" -> {
                            handleDocumentMessage(from, message.document)
                        }

                        else -> {
                            log.info("Received unsupported message type: {}", message.type)
                            whatsAppClient.sendTextMessage(from, "Default answer: I cannot handle your message.")
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok().build()
    }

    private fun handleTextMessage(
        from: String,
        body: String?,
    ) {
        val trimmed = body?.trim() ?: ""
        if (trimmed == "/id") {
            whatsAppClient.sendTextMessage(from, "Your WhatsApp ID (phone number) is: $from")
        } else {
            whatsAppClient.sendTextMessage(from, "Default answer: I cannot handle your message.")
        }
    }

    private fun handleDocumentMessage(
        from: String,
        document: WhatsAppDocument?,
    ) {
        if (document == null || document.id.isNullOrBlank() || document.filename.isNullOrBlank()) {
            whatsAppClient.sendTextMessage(from, "Invalid document received.")
            return
        }

        val mediaId = document.id
        val filename = document.filename

        log.info("Processing WhatsApp document: {}, media ID: {}", filename, mediaId)

        val mediaStream = whatsAppClient.downloadMedia(mediaId)
        if (mediaStream == null) {
            whatsAppClient.sendTextMessage(from, "Failed to download the document from WhatsApp servers.")
            return
        }

        mediaStream.use { inputStream ->
            val reply =
                botMessageDispatcher.processDocument(
                    fileName = filename,
                    inputStream = inputStream,
                )
            whatsAppClient.sendTextMessage(from, reply)
        }
    }
}

data class WhatsAppWebhookPayload(
    val `object`: String? = null,
    val entry: List<WhatsAppEntry>? = null,
)

data class WhatsAppEntry(
    val id: String? = null,
    val changes: List<WhatsAppChange>? = null,
)

data class WhatsAppChange(
    val value: WhatsAppValue? = null,
    val field: String? = null,
)

data class WhatsAppValue(
    @JsonProperty("messaging_product")
    val messagingProduct: String? = null,
    val metadata: WhatsAppMetadata? = null,
    val contacts: List<WhatsAppContact>? = null,
    val messages: List<WhatsAppMessage>? = null,
)

data class WhatsAppMetadata(
    @JsonProperty("display_phone_number")
    val displayPhoneNumber: String? = null,
    @JsonProperty("phone_number_id")
    val phoneNumberId: String? = null,
)

data class WhatsAppContact(
    val profile: WhatsAppProfile? = null,
    @JsonProperty("wa_id")
    val waId: String? = null,
)

data class WhatsAppProfile(
    val name: String? = null,
)

data class WhatsAppMessage(
    val from: String? = null,
    val id: String? = null,
    val timestamp: String? = null,
    val type: String? = null,
    val text: WhatsAppText? = null,
    val document: WhatsAppDocument? = null,
)

data class WhatsAppText(
    val body: String? = null,
)

data class WhatsAppDocument(
    val filename: String? = null,
    @JsonProperty("mime_type")
    val mimeType: String? = null,
    val sha256: String? = null,
    val id: String? = null,
)
