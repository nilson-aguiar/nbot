package dev.naguiar.nbot.tools.torrent

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nbot.qbittorrent")
data class QBittorrentProperties(
    val url: String,
    val username: String,
    val password: String,
    val useBuffering: Boolean = true,
)
