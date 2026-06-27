package dev.naguiar.nbot.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nbot.socialschools")
data class SocialSchoolsProperties(
    val username: String,
    val password: String,
    val url: String,
    val loginUrl: String,
    val downloadDir: String? = null,
)
