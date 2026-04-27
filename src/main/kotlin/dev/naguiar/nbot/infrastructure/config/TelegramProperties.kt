package dev.naguiar.nbot.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nbot.telegram")
data class TelegramProperties(
    val enabled: Boolean,
    val allowedUsers: List<Long> = emptyList(),
    val telegramBotToken: String,
)
