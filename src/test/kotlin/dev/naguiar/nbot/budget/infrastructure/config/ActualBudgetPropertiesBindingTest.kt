package dev.naguiar.nbot.budget.infrastructure.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["nbot.actual-budget.internal-accounts=Acc1,Acc2"])
class ActualBudgetPropertiesBindingTest {
    @Autowired
    private lateinit var properties: ActualBudgetProperties

    @Test
    fun `should bind internal accounts from properties`() {
        assertThat(properties.internalAccounts).containsExactly("Acc1", "Acc2")
    }
}
