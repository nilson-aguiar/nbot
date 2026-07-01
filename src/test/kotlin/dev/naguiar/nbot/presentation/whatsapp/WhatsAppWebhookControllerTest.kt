package dev.naguiar.nbot.presentation.whatsapp

import dev.naguiar.nbot.application.BotMessageDispatcher
import dev.naguiar.nbot.infrastructure.config.WhatsAppProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class WhatsAppWebhookControllerTest {
    private val whatsAppClient: WhatsAppClient = mockk(relaxed = true)
    private val botMessageDispatcher: BotMessageDispatcher = mockk()

    @Test
    fun `should verify webhook GET request successfully`() {
        val properties =
            WhatsAppProperties(
                enabled = true,
                allowedNumbers = listOf("123"),
                verifyToken = "secret-token",
                apiToken = "api-token",
                phoneNumberId = "phone-id",
            )
        val controller = WhatsAppWebhookController(properties, whatsAppClient, botMessageDispatcher)

        val response = controller.verifyWebhook("subscribe", "secret-token", "challenge-123")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("challenge-123", response.body)
    }

    @Test
    fun `should reject webhook GET request if token mismatches`() {
        val properties =
            WhatsAppProperties(
                enabled = true,
                allowedNumbers = listOf("123"),
                verifyToken = "secret-token",
                apiToken = "api-token",
                phoneNumberId = "phone-id",
            )
        val controller = WhatsAppWebhookController(properties, whatsAppClient, botMessageDispatcher)

        val response = controller.verifyWebhook("subscribe", "wrong-token", "challenge-123")

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `should process incoming WhatsApp text message from whitelisted number`() {
        val properties =
            WhatsAppProperties(
                enabled = true,
                allowedNumbers = listOf("12345"),
                verifyToken = "secret-token",
                apiToken = "api-token",
                phoneNumberId = "phone-id",
            )
        val controller = WhatsAppWebhookController(properties, whatsAppClient, botMessageDispatcher)

        val payload =
            WhatsAppWebhookPayload(
                entry =
                    listOf(
                        WhatsAppEntry(
                            changes =
                                listOf(
                                    WhatsAppChange(
                                        value =
                                            WhatsAppValue(
                                                messages =
                                                    listOf(
                                                        WhatsAppMessage(
                                                            from = "12345",
                                                            type = "text",
                                                            text = WhatsAppText(body = "Hello"),
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val response = controller.receiveWebhook(payload)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(exactly = 1) { whatsAppClient.sendTextMessage("12345", "Default answer: I cannot handle your message.") }
    }

    @Test
    fun `should skip incoming WhatsApp text message if number is not whitelisted`() {
        val properties =
            WhatsAppProperties(
                enabled = true,
                allowedNumbers = listOf("12345"),
                verifyToken = "secret-token",
                apiToken = "api-token",
                phoneNumberId = "phone-id",
            )
        val controller = WhatsAppWebhookController(properties, whatsAppClient, botMessageDispatcher)

        val payload =
            WhatsAppWebhookPayload(
                entry =
                    listOf(
                        WhatsAppEntry(
                            changes =
                                listOf(
                                    WhatsAppChange(
                                        value =
                                            WhatsAppValue(
                                                messages =
                                                    listOf(
                                                        WhatsAppMessage(
                                                            from = "99999", // not whitelisted
                                                            type = "text",
                                                            text = WhatsAppText(body = "Hello"),
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val response = controller.receiveWebhook(payload)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(exactly = 0) { whatsAppClient.sendTextMessage(any(), any()) }
    }

    @Test
    fun `should process incoming WhatsApp document message successfully`() {
        val properties =
            WhatsAppProperties(
                enabled = true,
                allowedNumbers = listOf("12345"),
                verifyToken = "secret-token",
                apiToken = "api-token",
                phoneNumberId = "phone-id",
                dashboardUrl = "http://localhost:8080",
            )
        val controller = WhatsAppWebhookController(properties, whatsAppClient, botMessageDispatcher)

        val payload =
            WhatsAppWebhookPayload(
                entry =
                    listOf(
                        WhatsAppEntry(
                            changes =
                                listOf(
                                    WhatsAppChange(
                                        value =
                                            WhatsAppValue(
                                                messages =
                                                    listOf(
                                                        WhatsAppMessage(
                                                            from = "12345",
                                                            type = "document",
                                                            document =
                                                                WhatsAppDocument(
                                                                    id = "media-789",
                                                                    filename = "statement.xml",
                                                                ),
                                                        ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val fakeInputStream = ByteArrayInputStream("xml data".toByteArray())
        every { whatsAppClient.downloadMedia("media-789") } returns fakeInputStream
        every { botMessageDispatcher.processDocument("statement.xml", any()) } returns
            "Imported successfully"

        val response = controller.receiveWebhook(payload)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(exactly = 1) { whatsAppClient.sendTextMessage("12345", "Imported successfully") }
    }
}
