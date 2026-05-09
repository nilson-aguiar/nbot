package dev.naguiar.nbot.budget.application

import dev.naguiar.nbot.budget.infrastructure.api.ActualAccount
import dev.naguiar.nbot.budget.infrastructure.api.ActualAccountResponse
import dev.naguiar.nbot.budget.infrastructure.api.ActualBudgetApi
import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActualBudgetServiceTest {

    private val api = mockk<ActualBudgetApi>()
    private val properties = ActualBudgetProperties(url = "http://localhost:5006", apiKey = "secret-key")
    private val service = ActualBudgetService(api, properties)

    @Test
    fun `should fetch accounts successfully`() {
        val expectedAccounts = listOf(
            ActualAccount("1", "Main Account", "bank", offbudget = false, closed = false)
        )
        every { api.getAccounts("secret-key") } returns ActualAccountResponse(expectedAccounts)

        val result = service.getAccounts()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Main Account")
    }
}
