package dev.naguiar.nbot.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nbot.security")
data class SecurityProperties(
    val allowedUsers: List<Long> = emptyList(),
    val telegramBotToken: String,
)
