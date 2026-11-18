package dev.naguiar.nbot.application.web

import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomepageServiceTest {
    private val ingressRouteService = mockk<IngressRouteService>()
    private val service = HomepageService(ingressRouteService)

    @Test
    fun `should group services by namespace and sort them`() {
        val routes =
            listOf(
                DiscoveredRoute(
                    name = "sonarr",
                    namespace = "media-flix",
                    hosts = listOf("sonarr.local.naguiar.dev", "sonarr.naguiar.dev"),
                    tls = true,
                ),
                DiscoveredRoute(
                    name = "radarr",
                    namespace = "media-flix",
                    hosts = listOf("radarr.local.naguiar.dev", "radarr.naguiar.dev"),
                    tls = true,
                ),
                DiscoveredRoute(
                    name = "grafana",
                    namespace = "monitoring",
                    hosts = listOf("grafana.local.naguiar.dev"),
                    tls = true,
                ),
                DiscoveredRoute(
                    name = "argocd",
                    namespace = "argocd",
                    hosts = listOf("argocd.local.naguiar.dev", "argocd.naguiar.dev"),
                    tls = false,
                ),
            )

        every { ingressRouteService.getRoutes(false) } returns
            IngressRouteResult(routes = routes, fetchedAt = Instant.now())
        every { ingressRouteService.resolvePrimaryHost(any(), "nbot.naguiar.dev") } answers {
            val hosts = firstArg<List<String>>()
            hosts.firstOrNull { !it.contains(".local.") } ?: hosts.first()
        }

        val data = service.getHomepageData("nbot.naguiar.dev")

        assertThat(data.totalServices).isEqualTo(4)
        assertThat(data.error).isNull()
        assertThat(data.groups.map { it.namespace }).containsExactly("argocd", "media-flix", "monitoring")

        val mediaServices = data.groups[1].services
        assertThat(mediaServices.map { it.name }).containsExactly("radarr", "sonarr")
        assertThat(mediaServices[0].url).isEqualTo("https://radarr.naguiar.dev")
        assertThat(mediaServices[0].secondaryHosts).containsExactly("radarr.local.naguiar.dev")

        val argoService = data.groups[0].services.first()
        assertThat(argoService.url).isEqualTo("http://argocd.naguiar.dev")
    }
}
