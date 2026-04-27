package dev.naguiar.nbot.tools.torrent

import dev.naguiar.nbot.infrastructure.config.QBittorrentConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile

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
    fun `addMagnetLink should return true and be visible in info`() {
        val result = client.addMagnetLink("magnet:?xt=urn:btih:5c3c147665711f229d60dc602282d49799f30ad3&tr=https://ipleak.net/announce.php%3Fh%3D5c3c147665711f229d60dc602282d49799f30ad3&dn=ipleak.net+torrent+detection")
        assertThat(result).isTrue()

        // Wait a bit for qBittorrent to process the addition
        Thread.sleep(2000)

        val torrents = client.getTorrentsInfo()
        assertThat(torrents).anyMatch {
            it["name"] == "ipleak.net torrent detection" ||
                (it["hash"] as String).equals("5c3c147665711f229d60dc602282d49799f30ad3", ignoreCase = true)
        }
    }

    @Test
    fun `addTorrentFile should return true and be visible in info`() {
        // Use the real test.torrent file provided by the user
        val file = ClassPathResource("test.torrent").file

        val result = client.addTorrentFile(file)
        assertThat(result).isTrue()

        // Wait a bit for qBittorrent to process the addition
        Thread.sleep(2000)

        val torrents = client.getTorrentsInfo()
        assertThat(torrents).isNotEmpty()
    }
}
