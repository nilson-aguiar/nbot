package dev.naguiar.nbot.tools.torrent

import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component

@Component
class TorrentTools(
    private val qBittorrentClient: QBittorrentClient,
) {
    private val log = LoggerFactory.getLogger(TorrentTools::class.java)

    @Tool(
        description =
            """
               Mandatory: Adds a torrent to qBittorrent using a magnet link. 
               Use this when the user provides a magnet link or requests a download from a link.
            """,
    )
    fun addTorrentMagnet(magnetLink: String): String {
        log.info("Invoking addTorrentMagnet tool with: {}", magnetLink)
        val success = qBittorrentClient.addMagnetLink(magnetLink)
        return if (success) {
            "Successfully added magnet link to qBittorrent."
        } else {
            "Failed to add magnet link to qBittorrent."
        }
    }

    @Tool(
        description =
            """
               Mandatory: Adds a .torrent file to qBittorrent using its absolute file path. 
               Use this when the user uploads or forwards a .torrent file.
            """,
    )
    fun addTorrentFile(filePath: String): String {
        log.info("Invoking addTorrentFile tool with path: {}", filePath)
        val file = java.io.File(filePath)
        if (!file.exists()) return "File not found."

        val success = qBittorrentClient.addTorrentFile(file)
        file.delete() // clean up
        return if (success) {
            "Successfully added the torrent file to qBittorrent."
        } else {
            "Failed to add the torrent file to qBittorrent."
        }
    }
}
