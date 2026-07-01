package dev.naguiar.nbot.application

import dev.naguiar.nbot.tools.torrent.TorrentTools
import java.io.File
import java.io.InputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BotMessageDispatcher(
    private val torrentTools: TorrentTools,
) {
    private val log = LoggerFactory.getLogger(BotMessageDispatcher::class.java)

    fun processDocument(
        fileName: String,
        inputStream: InputStream,
    ): String =
        try {
            if (fileName.endsWith(".torrent", ignoreCase = true)) {
                val tempFile = File.createTempFile("bot-torrent-", ".torrent")
                try {
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    torrentTools.addTorrentFile(tempFile.absolutePath)
                } finally {
                    tempFile.delete()
                }
            } else {
                "I only understand .torrent files at the moment."
            }
        } catch (e: Exception) {
            log.error("Failed to process document $fileName", e)
            "Failed to process the document: ${e.message}"
        }
}
