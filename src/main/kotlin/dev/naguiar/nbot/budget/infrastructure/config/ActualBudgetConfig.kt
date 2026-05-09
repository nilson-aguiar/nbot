package dev.naguiar.nbot.budget.infrastructure.config

import dev.naguiar.nbot.budget.infrastructure.api.ActualBudgetApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class ActualBudgetConfig {
    @Bean
    fun actualBudgetApi(
        builder: RestClient.Builder,
        properties: ActualBudgetProperties
    ): ActualBudgetApi {
        val client = builder.baseUrl(properties.url).build()
        val adapter = RestClientAdapter.create(client)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(ActualBudgetApi::class.java)
    }
}
