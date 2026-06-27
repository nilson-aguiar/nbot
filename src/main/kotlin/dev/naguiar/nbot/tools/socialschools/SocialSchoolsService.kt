package dev.naguiar.nbot.tools.socialschools

import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import java.nio.file.Files
import java.nio.file.Paths
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SocialSchoolsService(
    private val client: SocialSchoolsClient,
    properties: SocialSchoolsProperties,
) {
    private val log = LoggerFactory.getLogger(SocialSchoolsService::class.java)
    private val downloadDir =
        properties.downloadDir?.let { Paths.get(it) }
            ?: Paths.get(System.getProperty("user.home"), "socialschools")

    init {
        if (!Files.exists(downloadDir)) {
            Files.createDirectories(downloadDir)
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1 AM
    fun downloadNewFiles() {
        log.info("Starting SocialSchools download task")
        val downloaded = client.downloadAllMedia(downloadDir)
        log.info("Finished SocialSchools download task. Downloaded {} new files.", downloaded.size)
    }
}
