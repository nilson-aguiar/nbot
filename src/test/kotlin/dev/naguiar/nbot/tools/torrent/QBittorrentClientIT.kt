package dev.naguiar.nbot.tools.torrent

import dev.naguiar.nbot.infrastructure.config.QBittorrentConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile
import java.io.File

@SpringBootTest(classes = [QBittorrentClient::class, QBittorrentConfig::class, RestClientAutoConfiguration::class])
@EnableConfigurationProperties(QBittorrentProperties::class)
@Testcontainers
class QBittorrentClientIT {

    companion object {
        private val log = LoggerFactory.getLogger(QBittorrentClientIT::class.java)

        @Container
        val qbittorrent = GenericContainer("ghcr.io/home-operations/qbittorrent:5.1.4")
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("qBittorrent.conf", 0x1FF),
                "/config/qBittorrent.conf"
            )
            .withCreateContainerCmdModifier { cmd ->
                cmd.withEntrypoint("sh", "-c",
                    """
                        mkdir -p /config/qBittorrent && \
                        mv /config/qBittorrent.conf /config/qBittorrent/qBittorrent.conf && \
                        exec /usr/bin/catatonit -- /entrypoint.sh
                    """.trimIndent()
                )
            }
            .withEnv("WEBUI_PORT", "8080")
//            .withEnv("QBT_LEGAL_NOTICE", "confirm")
            .withLogConsumer(Slf4jLogConsumer(log))
            .waitingFor(Wait.forListeningPort())

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val host = qbittorrent.host
            val port = qbittorrent.getMappedPort(8080)
            registry.add("nbot.qbittorrent.url") { "http://$host:$port" }
            registry.add("nbot.qbittorrent.username") { "admin" }
            registry.add("nbot.qbittorrent.password") { "adminadmin" }
        }
    }

    @Autowired
    private lateinit var client: QBittorrentClient

    @Test
    fun `addMagnetLink should return true on success`() {
        val result = client.addMagnetLink("magnet:?xt=urn:btih:5c3c147665711f229d60dc602282d49799f30ad3&tr=https://ipleak.net/announce.php%3Fh%3D5c3c147665711f229d60dc602282d49799f30ad3&dn=ipleak.net+torrent+detection")
        assertThat(result).isTrue()
    }

    @Test
    fun `addTorrentFile should return true on success`() {
        val tempFile = File.createTempFile("test", ".torrent")
        // Minimal torrent file content might not be enough for qBittorrent to accept it,
        // but the HTTP request should still be 2xx if the format is correct.
        // However, qBittorrent might return 400 if it's not a valid bencoded torrent.
        // For IT purposes, we'll use a dummy text but qBittorrent might reject it.
        // Let's use a very basic but "valid-looking" multipart upload.
        tempFile.writeText("d8:announce12:udp://test.com4:infod6:lengthi1024e4:name4:test12:piece lengthi256e6:pieces20:00000000000000000000ee")

        val result = client.addTorrentFile(tempFile)
        assertThat(result).isTrue()

        tempFile.delete()
    }
}
