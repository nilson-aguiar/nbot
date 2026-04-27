# WebAPI Dashboard Testing Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement unit and integration tests for the dashboard components to maintain code coverage and verify behavior.

**Architecture:** Use MockK for unit tests and `MockMvc` for controller integration tests.

---

### Task 1: SseLogEmitterService Tests

**Files:**
- Create: `src/test/kotlin/dev/naguiar/nbot/infrastructure/logging/SseLogEmitterServiceTest.kt`

- [ ] **Step 1: Implement the test**

```kotlin
package dev.naguiar.nbot.infrastructure.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseLogEmitterServiceTest {
    private val service = SseLogEmitterService()

    @Test
    fun `should broadcast message to all emitters`() {
        val emitter1 = mockk<SseEmitter>(relaxed = true)
        val emitter2 = mockk<SseEmitter>(relaxed = true)
        
        service.addEmitter(emitter1)
        service.addEmitter(emitter2)
        
        service.broadcast("test-message")
        
        verify(exactly = 1) { emitter1.send(any()) }
        verify(exactly = 1) { emitter2.send(any()) }
    }

    @Test
    fun `should remove dead emitters on broadcast failure`() {
        val emitter = mockk<SseEmitter>()
        every { emitter.send(any()) } throws Exception("Dead")
        
        service.addEmitter(emitter)
        service.broadcast("test")
        
        // Second broadcast should not attempt to send to the dead emitter
        service.broadcast("test2")
        
        verify(exactly = 1) { emitter.send(any()) }
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew test --tests SseLogEmitterServiceTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/dev/naguiar/nbot/infrastructure/logging/SseLogEmitterServiceTest.kt
git commit -m "test: add SseLogEmitterServiceTest"
```

---

### Task 2: DashboardDataService Tests

**Files:**
- Create: `src/test/kotlin/dev/naguiar/nbot/application/web/DashboardDataServiceTest.kt`

- [ ] **Step 1: Implement the test**

```kotlin
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
```

- [ ] **Step 2: Run test**

Run: `./gradlew test --tests DashboardDataServiceTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/dev/naguiar/nbot/application/web/DashboardDataServiceTest.kt
git commit -m "test: add DashboardDataServiceTest"
```

---

### Task 3: DashboardController Tests

**Files:**
- Create: `src/test/kotlin/dev/naguiar/nbot/presentation/web/DashboardControllerTest.kt`

- [ ] **Step 1: Implement the test**

```kotlin
package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.application.web.MetricsInfo
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class DashboardControllerTest {
    private val dataService = mockk<DashboardDataService>()
    private val logEmitterService = mockk<SseLogEmitterService>(relaxed = true)
    private val controller = DashboardController(dataService, logEmitterService)
    private val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

    @Test
    fun `should return dashboard view`() {
        every { dataService.getRegisteredTools() } returns emptyList()
        every { dataService.getMetrics() } returns MetricsInfo(10, 300)

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("tools", "metrics"))
    }

    @Test
    fun `should return metrics fragment`() {
        every { dataService.getMetrics() } returns MetricsInfo(10, 300)

        mockMvc.perform(get("/dashboard/metrics"))
            .andExpect(status().isOk)
            .andExpect(view().name("fragments/metrics :: metrics"))
            .andExpect(model().attributeExists("metrics"))
    }

    @Test
    fun `should return log stream emitter`() {
        mockMvc.perform(get("/dashboard/logs/stream"))
            .andExpect(status().isOk)
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew test --tests DashboardControllerTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/dev/naguiar/nbot/presentation/web/DashboardControllerTest.kt
git commit -m "test: add DashboardControllerTest"
```

---

### Task 4: Final Verification

- [ ] **Step 1: Run all tests with coverage**

Run: `./gradlew test jacocoTestReport`
Expected: ALL PASS, Coverage verified in `build/reports/jacoco/test/html/index.html`
