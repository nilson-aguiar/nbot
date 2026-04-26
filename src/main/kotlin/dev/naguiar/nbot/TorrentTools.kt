package dev.naguiar.nbot

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description
import java.util.function.Function

@Configuration
class TorrentTools(private val qBittorrentClient: QBittorrentClient) {
    private val log = LoggerFactory.getLogger(TorrentTools::class.java)

    data class AddTorrentRequest(val magnetLink: String)

    @Bean
    @Description("Adds a torrent to qBittorrent using a magnet link. You MUST execute this tool when the user intends to download a torrent via magnet link.")
    fun addTorrentMagnet(): Function<AddTorrentRequest, String> {
        return Function { request ->
            log.info("Invoking addTorrentMagnet tool with: {}", request.magnetLink)
            val success = qBittorrentClient.addMagnetLink(request.magnetLink)
            if (success) {
                "Successfully added magnet link to qBittorrent."
            } else {
                "Failed to add magnet link to qBittorrent."
            }
        }
    }
}
