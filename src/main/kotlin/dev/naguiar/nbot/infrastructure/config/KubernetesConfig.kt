package dev.naguiar.nbot.infrastructure.config

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "nbot.homepage")
data class HomepageProperties(
    val enabled: Boolean = true,
    val cacheTtlSeconds: Long = 30,
)

@Configuration
class KubernetesConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "nbot.homepage", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun kubernetesClient(): KubernetesClient = KubernetesClientBuilder().build()
}
