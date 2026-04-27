package dev.naguiar.nbot.infrastructure.config

import dev.naguiar.nbot.tools.torrent.QBittorrentApi
import dev.naguiar.nbot.tools.torrent.QBittorrentProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class QBittorrentConfig {
    @Bean
    fun qBittorrentApi(properties: QBittorrentProperties): QBittorrentApi {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            @Suppress("DEPRECATION")
            setBufferRequestBody(true)
        }
        val client = RestClient.builder()
            .baseUrl(properties.url)
            .requestFactory(requestFactory)
            .build()
        val adapter = RestClientAdapter.create(client)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(QBittorrentApi::class.java)
    }
}
