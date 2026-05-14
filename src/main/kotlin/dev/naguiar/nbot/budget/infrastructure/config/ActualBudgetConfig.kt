package dev.naguiar.nbot.budget.infrastructure.config

import dev.naguiar.nbot.budget.infrastructure.api.generated.AccountsApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.PayeesApi
import dev.naguiar.nbot.budget.infrastructure.api.generated.TransactionsApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class ActualBudgetConfig {

    @Bean
    fun accountsApi(builder: RestClient.Builder, properties: ActualBudgetProperties): AccountsApi {
        return createClient(builder, properties.url, properties.apiKey, AccountsApi::class.java)
    }

    @Bean
    fun payeesApi(builder: RestClient.Builder, properties: ActualBudgetProperties): PayeesApi {
        return createClient(builder, properties.url, properties.apiKey, PayeesApi::class.java)
    }

    @Bean
    fun transactionsApi(builder: RestClient.Builder, properties: ActualBudgetProperties): TransactionsApi {
        return createClient(builder, properties.url, properties.apiKey, TransactionsApi::class.java)
    }

    private fun <T> createClient(builder: RestClient.Builder, baseUrl: String, apiKey: String, clazz: Class<T>): T {
        val finalBaseUrl = if (baseUrl.endsWith("/v1")) baseUrl else "${baseUrl.removeSuffix("/")}/v1"
        val client = builder.baseUrl(finalBaseUrl)
            .requestInterceptor { request, body, execution ->
                request.headers.add("x-api-key", apiKey)
                execution.execute(request, body)
            }
            .build()
        val adapter = RestClientAdapter.create(client)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(clazz)
    }
}
