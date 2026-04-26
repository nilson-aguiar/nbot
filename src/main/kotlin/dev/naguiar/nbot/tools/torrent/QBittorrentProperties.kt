package dev.naguiar.nbot.tools.torrent

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "nbot.qbittorrent")
class QBittorrentProperties {
    var url: String = ""
    var username: String = ""
    var password: String = ""
}
