package dev.naguiar.nbot.application.web

import dev.naguiar.nbot.infrastructure.config.HomepageProperties
import io.fabric8.kubernetes.api.model.GenericKubernetesResource
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation
import io.fabric8.kubernetes.client.dsl.Resource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider

class IngressRouteServiceTest {
    private val client = mockk<KubernetesClient>()
    private val clientProvider = mockk<ObjectProvider<KubernetesClient>>()
    private val fixedInstant = Instant.parse("2026-11-18T19:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    @Test
    fun `should parse single and multi-route IngressRoutes`() {
        val sonarr =
            ingressRoute(
                name = "sonarr",
                namespace = "media-flix",
                entryPoints = listOf("websecure"),
                hasTls = true,
                matches = listOf("Host(`sonarr.local.naguiar.dev`) || Host(`sonarr.naguiar.dev`)"),
            )
        val homeAssistant =
            ingressRoute(
                name = "home-assistant",
                namespace = "home-assistant",
                entryPoints = listOf("websecure"),
                hasTls = true,
                matches =
                    listOf(
                        "Host(`ha.local.naguiar.dev`) || Host(`ha.naguiar.dev`)",
                        "Host(`z2m.local.naguiar.dev`)",
                    ),
            )
        val httpOnly =
            ingressRoute(
                name = "otlp",
                namespace = "monitoring",
                entryPoints = listOf("web"),
                hasTls = false,
                matches = listOf("Host(`otlp.local.naguiar.dev`)"),
            )

        stubClusterResources(listOf(sonarr, homeAssistant, httpOnly))

        val service =
            IngressRouteService(
                clientProvider,
                HomepageProperties(enabled = true, cacheTtlSeconds = 30),
                clock,
            )
        val result = service.getRoutes()

        assertThat(result.error).isNull()
        assertThat(result.routes).hasSize(4)
        assertThat(result.routes[0]).isEqualTo(
            DiscoveredRoute(
                name = "sonarr",
                namespace = "media-flix",
                hosts = listOf("sonarr.local.naguiar.dev", "sonarr.naguiar.dev"),
                tls = true,
            ),
        )
        assertThat(result.routes[1].hosts).containsExactly("ha.local.naguiar.dev", "ha.naguiar.dev")
        assertThat(result.routes[2].hosts).containsExactly("z2m.local.naguiar.dev")
        assertThat(result.routes[3].tls).isFalse()
    }

    @Test
    fun `should cache routes within TTL and refresh when forced`() {
        val sonarr =
            ingressRoute(
                name = "sonarr",
                namespace = "media-flix",
                entryPoints = listOf("websecure"),
                hasTls = true,
                matches = listOf("Host(`sonarr.local.naguiar.dev`)"),
            )
        val operation = stubClusterResources(listOf(sonarr))

        val service =
            IngressRouteService(
                clientProvider,
                HomepageProperties(enabled = true, cacheTtlSeconds = 30),
                clock,
            )

        service.getRoutes()
        service.getRoutes()
        verify(exactly = 1) { operation.list() }

        service.getRoutes(forceRefresh = true)
        verify(exactly = 2) { operation.list() }
    }

    @Test
    fun `should return error result when kubernetes client is unavailable`() {
        every { clientProvider.ifAvailable } returns null

        val service = IngressRouteService(clientProvider, HomepageProperties(enabled = true), clock)
        val result = service.getRoutes()

        assertThat(result.routes).isEmpty()
        assertThat(result.error).contains("not available")
    }

    @Test
    fun `should return error result when kubernetes query throws`() {
        every { clientProvider.ifAvailable } returns client
        every {
            client.genericKubernetesResources("traefik.io/v1alpha1", "IngressRoute")
        } throws RuntimeException("forbidden")

        val service = IngressRouteService(clientProvider, HomepageProperties(enabled = true), clock)
        val result = service.getRoutes()

        assertThat(result.routes).isEmpty()
        assertThat(result.error).contains("forbidden")
    }

    @Test
    fun `should resolve primary host matching request domain`() {
        val service = IngressRouteService(clientProvider, HomepageProperties(), clock)
        val hosts = listOf("sonarr.local.naguiar.dev", "sonarr.naguiar.dev")

        assertThat(service.resolvePrimaryHost(hosts, "nbot.local.naguiar.dev")).isEqualTo("sonarr.local.naguiar.dev")
        assertThat(service.resolvePrimaryHost(hosts, "nbot.naguiar.dev")).isEqualTo("sonarr.naguiar.dev")
        assertThat(service.resolvePrimaryHost(hosts, "nbot.naguiar.dev:8443")).isEqualTo("sonarr.naguiar.dev")
        assertThat(service.resolvePrimaryHost(hosts, "localhost")).isEqualTo("sonarr.local.naguiar.dev")
        assertThat(service.resolvePrimaryHost(listOf("prowlarr.local.naguiar.dev"), "nbot.naguiar.dev"))
            .isEqualTo("prowlarr.local.naguiar.dev")
    }

    @Suppress("UNCHECKED_CAST")
    private fun stubClusterResources(
        items: List<GenericKubernetesResource>,
    ): NonNamespaceOperation<
        GenericKubernetesResource,
        GenericKubernetesResourceList,
        Resource<GenericKubernetesResource>,
    > {
        val mixedOp =
            mockk<
                MixedOperation<
                    GenericKubernetesResource,
                    GenericKubernetesResourceList,
                    Resource<GenericKubernetesResource>,
                >,
            >()
        val anyNsOp =
            mockk<
                NonNamespaceOperation<
                    GenericKubernetesResource,
                    GenericKubernetesResourceList,
                    Resource<GenericKubernetesResource>,
                >,
            >()
        val list =
            GenericKubernetesResourceList().apply {
                this.items = items
            }

        every { clientProvider.ifAvailable } returns client
        every { client.genericKubernetesResources("traefik.io/v1alpha1", "IngressRoute") } returns mixedOp
        every { mixedOp.inAnyNamespace() } returns anyNsOp
        every { anyNsOp.list() } returns list

        return anyNsOp
    }

    private fun ingressRoute(
        name: String,
        namespace: String,
        entryPoints: List<String>,
        hasTls: Boolean,
        matches: List<String>,
    ): GenericKubernetesResource {
        val spec =
            mutableMapOf<String, Any>(
                "entryPoints" to entryPoints,
                "routes" to matches.map { mapOf("match" to it, "kind" to "Rule") },
            )
        if (hasTls) {
            spec["tls"] = mapOf("secretName" to "certificate-tls")
        }

        return GenericKubernetesResource().apply {
            metadata =
                ObjectMeta().apply {
                    this.name = name
                    this.namespace = namespace
                }
            additionalProperties = mutableMapOf("spec" to spec)
        }
    }
}
