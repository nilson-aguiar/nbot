package dev.naguiar.nbot.application.web

import org.springframework.stereotype.Service

data class ServiceCard(
    val name: String,
    val namespace: String,
    val primaryHost: String,
    val url: String,
    val tls: Boolean,
    val secondaryHosts: List<String>,
)

data class NamespaceGroup(
    val namespace: String,
    val services: List<ServiceCard>,
)

data class HomepageData(
    val groups: List<NamespaceGroup>,
    val totalServices: Int,
    val error: String? = null,
)

@Service
class HomepageService(
    private val ingressRouteService: IngressRouteService,
) {
    fun getHomepageData(
        requestHost: String?,
        forceRefresh: Boolean = false,
    ): HomepageData {
        val result = ingressRouteService.getRoutes(forceRefresh)
        val cards =
            result.routes.map { route ->
                val primaryHost = ingressRouteService.resolvePrimaryHost(route.hosts, requestHost)
                val scheme = if (route.tls) "https" else "http"
                ServiceCard(
                    name = deriveDisplayName(primaryHost, route.name),
                    namespace = route.namespace,
                    primaryHost = primaryHost,
                    url = "$scheme://$primaryHost",
                    tls = route.tls,
                    secondaryHosts = route.hosts.filter { it != primaryHost },
                )
            }

        val groups =
            cards
                .groupBy { it.namespace }
                .toSortedMap()
                .map { (namespace, services) ->
                    NamespaceGroup(
                        namespace = namespace,
                        services = services.sortedWith(compareBy({ it.name }, { it.primaryHost })),
                    )
                }

        return HomepageData(
            groups = groups,
            totalServices = cards.size,
            error = result.error,
        )
    }

    private fun deriveDisplayName(
        primaryHost: String,
        fallbackName: String,
    ): String {
        val subdomain = primaryHost.substringBefore(".")
        return subdomain.ifBlank { fallbackName }
    }
}
