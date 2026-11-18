package dev.naguiar.nbot.application.web

import dev.naguiar.nbot.tools.BotTool
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext

class DashboardDataServiceTest {
    private val ctx = mockk<ApplicationContext>()
    private val service = DashboardDataService(ctx)

    class SampleToolBean {
        @BotTool(description = "Sample description")
        fun sampleTool(): String = "ok"

        fun nonToolMethod(): String = "ignored"
    }

    @Test
    fun `should discover methods annotated with BotTool`() {
        every { ctx.beanDefinitionNames } returns arrayOf("sampleToolBean", "brokenBean")
        every { ctx.getBean("sampleToolBean") } returns SampleToolBean()
        every { ctx.getBean("brokenBean") } throws RuntimeException("Cannot instantiate")

        val tools = service.getRegisteredTools()

        assertEquals(listOf(ToolInfo("sampleTool", "Sample description")), tools)
    }
}
