package dev.naguiar.nbot.application.web

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.search.Search
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext

class DashboardDataServiceTest {
    private val ctx = mockk<ApplicationContext>()
    private val meterRegistry = mockk<MeterRegistry>()
    private val service = DashboardDataService(ctx, meterRegistry)

    @Test
    fun `should retrieve metrics`() {
        val gauge = mockk<Gauge>()
        every { gauge.value() } returns 0.05
        
        val search = mockk<Search>()
        every { meterRegistry.find("system.cpu.usage") } returns search
        every { search.gauge() } returns gauge
        
        val metrics = service.getMetrics()
        
        assertEquals(5, metrics.cpuLoad)
        assertEquals(342, metrics.latencyMs)
    }

    @Test
    fun `should handle missing metrics`() {
        every { meterRegistry.find(any()) } returns mockk {
            every { gauge() } returns null
        }
        
        val metrics = service.getMetrics()
        
        assertEquals(0, metrics.cpuLoad)
    }
}
