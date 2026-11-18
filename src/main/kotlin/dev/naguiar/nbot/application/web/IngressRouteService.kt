package dev.naguiar.nbot.application.web

import dev.naguiar.nbot.infrastructure.config.HomepageProperties
import io.fabric8.kubernetes.api.model.GenericKubernetesResource
import io.fabric8.kubernetes.client.KubernetesClient
import java.time.Clock
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

data class DiscoveredRoute(
    val name: String,
    val namespace: String,
    val hosts: List<String>,
    val tls: Boolean,
)

data class IngressRouteResult(
    val routes: List<DiscoveredRoute>,
    val error: String? = null,
    val fetchedAt: Instant,
)

@Service
class IngressRouteService(
    private val kubernetesClientProvider: ObjectProvider<KubernetesClient>,
    private val properties: HomepageProperties,
    private val clock: Clock,
) {
    @Autowired
    constructor(
        kubernetesClientProvider: ObjectProvider<KubernetesClient>,
        properties: HomepageProperties,
    ) : this(kubernetesClientProvider, properties, Clock.systemUTC())

    private val logger = LoggerFactory.getLogger(IngressRouteService::class.java)
    private val lock = ReentrantLock()
    private var cached: IngressRouteResult? = null

    fun getRoutes(forceRefresh: Boolean = false): IngressRouteResult {
        val now = Instant.now(clock)
        val current = cached
        if (!forceRefresh && current != null && !isExpired(current, now)) {
            return current
        }

        return lock.withLock {
            val rechecked = cached
            val lockedNow = Instant.now(clock)
            if (!forceRefresh && rechecked != null && !isExpired(rechecked, lockedNow)) {
                return@withLock rechecked
            }
            fetchRoutes(lockedNow).also { cached = it }
        }
    }

    fun resolvePrimaryHost(
        hosts: List<String>,
        requestHost: String?,
    ): String {
        val cleanRequestHost =
            requestHost
                ?.substringBefore(":")
                ?.trim()
                ?.lowercase()
                .orEmpty()
        if (cleanRequestHost.isNotBlank()) {
            if (cleanRequestHost.contains(".local.")) {
                hosts.firstOrNull { it.contains(".local.") }?.let { return it }
            } else if (cleanRequestHost.contains(".") && cleanRequestHost != "127.0.0.1") {
                hosts.firstOrNull { !it.contains(".local.") }?.let { return it }
            }
        }
        return hosts.first()
    }

    private fun isExpired(
        result: IngressRouteResult,
        now: Instant,
    ): Boolean = result.fetchedAt.plusSeconds(properties.cacheTtlSeconds).isBefore(now)

    private fun fetchRoutes(now: Instant): IngressRouteResult {
        if (!properties.enabled) {
            return IngressRouteResult(emptyList(), error = "Homepage discovery is disabled.", fetchedAt = now)
        }

        val client =
            kubernetesClientProvider.ifAvailable
                ?: return IngressRouteResult(
                    emptyList(),
                    error = "Kubernetes client is not available.",
                    fetchedAt = now,
                )

        return try {
            val items =
                client
                    .genericKubernetesResources(TRAEFIK_API_VERSION, INGRESS_ROUTE_KIND)
                    .inAnyNamespace()
                    .list()
                    ?.items
                    .orEmpty()

            val routes = items.flatMap(::parseIngressRoute)
            IngressRouteResult(routes = routes, fetchedAt = now)
        } catch (ex: Exception) {
            logger.warn("Failed to list IngressRoutes from Kubernetes cluster: {}", ex.message)
            IngressRouteResult(
                routes = emptyList(),
                error = "Unable to load IngressRoutes from cluster: ${ex.message ?: "unknown error"}",
                fetchedAt = now,
            )
        }
    }

    private fun parseIngressRoute(resource: GenericKubernetesResource): List<DiscoveredRoute> {
        val namespace = resource.metadata?.namespace?.takeIf { it.isNotBlank() } ?: "default"
        val name = resource.metadata?.name?.takeIf { it.isNotBlank() } ?: "unknown"
        val spec = resource.additionalProperties["spec"] as? Map<*, *> ?: return emptyList()

        val entryPoints = (spec["entryPoints"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
        val tls = spec["tls"] != null || entryPoints.contains("websecure")
        val rawRoutes = spec["routes"] as? List<*> ?: return emptyList()

        return rawRoutes.mapNotNull { entry ->
            val routeMap = entry as? Map<*, *> ?: return@mapNotNull null
            val match = routeMap["match"] as? String ?: return@mapNotNull null
            val hosts = extractHosts(match)
            if (hosts.isEmpty()) {
                null
            } else {
                DiscoveredRoute(
                    name = name,
                    namespace = namespace,
                    hosts = hosts,
                    tls = tls,
                )
            }
        }
    }

    private fun extractHosts(matchExpression: String): List<String> =
        HOST_PATTERN
            .findAll(matchExpression)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

    companion object {
        private const val TRAEFIK_API_VERSION = "traefik.io/v1alpha1"
        private const val INGRESS_ROUTE_KIND = "IngressRoute"
        private val HOST_PATTERN = Regex("""Host\([`"']([^`"']+)[`"']\)""")
    }
}
