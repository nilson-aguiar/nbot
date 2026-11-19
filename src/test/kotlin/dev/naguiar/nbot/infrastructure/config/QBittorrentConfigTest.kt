package dev.naguiar.nbot.infrastructure.config

import dev.naguiar.nbot.tools.torrent.QBittorrentApi
import dev.naguiar.nbot.tools.torrent.QBittorrentProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

class QBittorrentConfigTest {
    @Configuration
    @EnableConfigurationProperties(QBittorrentProperties::class)
    class TestPropertiesConfig

    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration::class.java))
            .withUserConfiguration(TestPropertiesConfig::class.java, QBittorrentConfig::class.java)
            .withPropertyValues(
                "nbot.qbittorrent.url=http://localhost:8090",
                "nbot.qbittorrent.username=admin",
                "nbot.qbittorrent.password=adminadmin",
            )

    @Test
    fun `should auto-configure RestClient Builder and create QBittorrentApi bean`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(RestClient.Builder::class.java)
            assertThat(context).hasSingleBean(QBittorrentApi::class.java)
        }
    }
}
