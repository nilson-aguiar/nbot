package dev.naguiar.nbot.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "nbot.security")
class SecurityProperties {
    var allowedUsers: List<Long> = emptyList()
    var telegramBotToken: String = ""
}
