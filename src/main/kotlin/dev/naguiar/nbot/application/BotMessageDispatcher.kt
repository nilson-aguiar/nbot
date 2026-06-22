package dev.naguiar.nbot.application

import dev.naguiar.nbot.budget.application.BudgetImportService
import dev.naguiar.nbot.tools.torrent.TorrentTools
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

@Service
class BotMessageDispatcher(
    private val torrentTools: TorrentTools,
    private val budgetImportService: BudgetImportService,
) {
    private val log = LoggerFactory.getLogger(BotMessageDispatcher::class.java)

    fun processDocument(
        fileName: String,
        inputStream: InputStream,
        dashboardUrl: String,
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
            } else if (fileName.endsWith(".xml", ignoreCase = true)) {
                budgetImportService.importCamt(inputStream)
                "Successfully imported transactions. Review them on the dashboard: $dashboardUrl/dashboard"
            } else {
                "I only understand .torrent and .xml files at the moment."
            }
        } catch (e: Exception) {
            log.error("Failed to process document $fileName", e)
            "Failed to process the document: ${e.message}"
        }
}
