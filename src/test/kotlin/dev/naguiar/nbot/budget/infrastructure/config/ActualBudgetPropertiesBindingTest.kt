package dev.naguiar.nbot.budget.infrastructure.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ActualBudgetPropertiesBindingTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(TestConfig::class.java)

    @Test
    fun `should bind internal accounts from properties`() {
        contextRunner
            .withPropertyValues("nbot.actual-budget.internal-accounts=Acc1,Acc2")
            .run { context ->
                val properties = context.getBean(ActualBudgetProperties::class.java)
                assertThat(properties.internalAccounts).containsExactly("Acc1", "Acc2")
            }
    }

    @Configuration
    @EnableConfigurationProperties(ActualBudgetProperties::class)
    class TestConfig
}
