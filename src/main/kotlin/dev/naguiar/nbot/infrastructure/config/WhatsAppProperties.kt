package dev.naguiar.nbot.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nbot.whatsapp")
data class WhatsAppProperties(
    val enabled: Boolean,
    val allowedNumbers: List<String> = emptyList(),
    val verifyToken: String,
    val apiToken: String,
    val phoneNumberId: String,
    val dashboardUrl: String = "http://localhost:8080",
)
