package dev.naguiar.nbot.infrastructure.config

import dev.naguiar.nbot.tools.torrent.QBittorrentApi
import dev.naguiar.nbot.tools.torrent.QBittorrentProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

@Configuration
class QBittorrentConfig {
    @Bean
    @ConditionalOnProperty(name = ["nbot.qbittorrent.use-buffering"], havingValue = "true", matchIfMissing = true)
    fun qBittorrentRequestFactory(): ClientHttpRequestFactory =
        BufferingClientHttpRequestFactory(JdkClientHttpRequestFactory())

    @Bean
    fun qBittorrentApi(
        properties: QBittorrentProperties,
        builder: RestClient.Builder,
        requestFactory: ClientHttpRequestFactory?,
    ): QBittorrentApi {
        // If a request factory bean exists (i.e., not in test), use it
        requestFactory?.let {
            builder.requestFactory(it)
        }

        val qBittorrentRestClient =
            builder
                .baseUrl(properties.url)
                .defaultHeader(HttpHeaders.CONNECTION, "close")
                .build()

        val adapter = RestClientAdapter.create(qBittorrentRestClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient<QBittorrentApi>()
    }
}
