# WebAPI Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a real-time system monitoring dashboard using Thymeleaf and HTMX, styled like the provided mockup, with live Actuator metrics and SSE-streamed logs.

**Architecture:** A Spring MVC Controller serves the main dashboard and fragments. A `DashboardDataService` aggregates metrics and `@Tool` annotations. A custom Logback Appender broadcasts logs via `SseEmitter`.

**Tech Stack:** Kotlin, Spring Boot 3.5.x, Thymeleaf, HTMX, Tailwind CSS (via CDN for simplicity), Logback.

---

### Task 1: Project Setup & Dependencies

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add Thymeleaf dependency**

```kotlin
dependencies {
    // ... existing
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    // ...
}
```

- [ ] **Step 2: Refresh Gradle**

Run: `./gradlew classes`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: add thymeleaf dependency"
```

---

### Task 2: SSE Log Emitter Service

**Files:**
- Create: `src/main/kotlin/dev/naguiar/nbot/infrastructure/logging/SseLogEmitterService.kt`

- [ ] **Step 1: Implement the emitter service**

```kotlin
package dev.naguiar.nbot.infrastructure.logging

import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SseLogEmitterService {
    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    fun addEmitter(emitter: SseEmitter) {
        emitters.add(emitter)
        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
    }

    fun broadcast(message: String) {
        val deadEmitters = mutableListOf<SseEmitter>()
        emitters.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name("message").data(message))
            } catch (e: Exception) {
                deadEmitters.add(emitter)
            }
        }
        emitters.removeAll(deadEmitters)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/infrastructure/logging/SseLogEmitterService.kt
git commit -m "feat: add SseLogEmitterService"
```

---

### Task 3: Custom Logback Appender

**Files:**
- Create: `src/main/kotlin/dev/naguiar/nbot/infrastructure/logging/SseLogbackAppender.kt`
- Create: `src/main/resources/logback-spring.xml`

- [ ] **Step 1: Implement the Logback Appender**

```kotlin
package dev.naguiar.nbot.infrastructure.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class SseLogbackAppender : AppenderBase<ILoggingEvent>(), ApplicationContextAware {
    private lateinit var ctx: ApplicationContext
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.ctx = applicationContext
    }

    override fun append(event: ILoggingEvent) {
        if (!::ctx.isInitialized) return
        
        val emitterService = ctx.getBean(SseLogEmitterService::class.java)
        val time = formatter.format(Instant.ofEpochMilli(event.timeStamp))
        val level = event.level.toString()
        val colorClass = when (level) {
            "ERROR" -> "text-red-500"
            "WARN" -> "text-yellow-500"
            else -> "text-green-500"
        }
        
        val html = """<div class="font-mono text-sm mb-1">
            <span class="text-gray-500">[$time]</span> 
            <span class="$colorClass">[$level]</span> 
            <span class="text-gray-300">${event.formattedMessage}</span>
        </div>"""
        
        emitterService.broadcast(html)
    }
}
```

- [ ] **Step 2: Configure Logback**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <appender name="SSE" class="dev.naguiar.nbot.infrastructure.logging.SseLogbackAppender"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="SSE"/>
    </root>
</configuration>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/infrastructure/logging/SseLogbackAppender.kt src/main/resources/logback-spring.xml
git commit -m "feat: configure SseLogbackAppender"
```

---

### Task 4: Dashboard Data Service

**Files:**
- Create: `src/main/kotlin/dev/naguiar/nbot/application/web/DashboardDataService.kt`

- [ ] **Step 1: Implement data aggregation**

```kotlin
package dev.naguiar.nbot.application.web

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.ai.tool.annotation.Tool
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.lang.reflect.Method
import kotlin.math.roundToInt

data class ToolInfo(val name: String, val description: String)
data class MetricsInfo(val cpuLoad: Int, val latencyMs: Int)

@Service
class DashboardDataService(
    private val ctx: ApplicationContext,
    private val meterRegistry: MeterRegistry
) {
    fun getRegisteredTools(): List<ToolInfo> {
        val tools = mutableListOf<ToolInfo>()
        val beans = ctx.getBeansWithAnnotation(Service::class.java) // Simplify for now
        beans.values.forEach { bean ->
            bean.javaClass.methods.forEach { method ->
                method.getAnnotation(Tool::class.java)?.let { annotation ->
                    tools.add(ToolInfo(method.name, annotation.description))
                }
            }
        }
        return tools
    }

    fun getMetrics(): MetricsInfo {
        val cpu = meterRegistry.find("system.cpu.usage").gauge()?.value() ?: 0.0
        return MetricsInfo(
            cpuLoad = (cpu * 100).roundToInt(),
            latencyMs = 342 // Mocked for now as actuator latency requires custom filter
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/application/web/DashboardDataService.kt
git commit -m "feat: add DashboardDataService"
```

---

### Task 5: Dashboard Controller

**Files:**
- Create: `src/main/kotlin/dev/naguiar/nbot/presentation/web/DashboardController.kt`

- [ ] **Step 1: Implement the controller**

```kotlin
package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Controller
class DashboardController(
    private val dataService: DashboardDataService,
    private val logEmitterService: SseLogEmitterService
) {
    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        model.addAttribute("tools", dataService.getRegisteredTools())
        model.addAttribute("metrics", dataService.getMetrics())
        return "dashboard"
    }

    @GetMapping("/dashboard/metrics")
    fun metricsFragment(model: Model): String {
        model.addAttribute("metrics", dataService.getMetrics())
        return "fragments/metrics :: metrics"
    }

    @GetMapping("/dashboard/logs/stream")
    fun logStream(): SseEmitter {
        val emitter = SseEmitter(-1L)
        logEmitterService.addEmitter(emitter)
        return emitter
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/dev/naguiar/nbot/presentation/web/DashboardController.kt
git commit -m "feat: add DashboardController"
```

---

### Task 6: Thymeleaf Templates

**Files:**
- Create: `src/main/resources/templates/dashboard.html`
- Create: `src/main/resources/templates/fragments/metrics.html`

- [ ] **Step 1: Implement dashboard.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Gemini Torrents Dashboard</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <script src="https://unpkg.com/htmx.org/dist/ext/sse.js"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body { background-color: #0d1117; color: #c9d1d9; }
        .card { background-color: #161b22; border: 1px solid #30363d; border-radius: 0.5rem; }
    </style>
</head>
<body class="p-8">
    <div class="max-w-7xl mx-auto flex gap-8">
        <!-- Sidebar -->
        <div class="w-80 flex flex-col gap-6">
            <div class="card p-6">
                <h2 class="text-sm font-semibold uppercase tracking-wider text-gray-400 mb-4">Infrastructure</h2>
                <div id="metrics-container" hx-get="/dashboard/metrics" hx-trigger="every 2s">
                    <div th:replace="~{fragments/metrics :: metrics}"></div>
                </div>
            </div>

            <div class="card p-6 flex-grow">
                <h2 class="text-sm font-semibold uppercase tracking-wider text-gray-400 mb-4">Registered Tools</h2>
                <ul class="space-y-3">
                    <li th:each="tool : ${tools}" class="flex items-center gap-2">
                        <span class="w-2 h-2 bg-green-500 rounded-full"></span>
                        <span th:text="${tool.name}" class="font-mono text-sm text-gray-300"></span>
                    </li>
                </ul>
            </div>
        </div>

        <!-- Main Content -->
        <div class="flex-grow flex flex-col gap-6">
            <div class="card flex-grow flex flex-col overflow-hidden">
                <div class="p-4 border-b border-gray-700 flex justify-between items-center">
                    <h2 class="text-sm font-semibold uppercase tracking-wider text-gray-400">Live Execution Logs</h2>
                </div>
                <div class="p-4 flex-grow overflow-y-auto bg-black font-mono text-sm" 
                     hx-ext="sse" sse-connect="/dashboard/logs/stream" sse-swap="message" hx-swap="beforeend">
                    <div class="text-gray-500 italic">Waiting for logs...</div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 2: Implement metrics.html fragment**

```html
<div th:fragment="metrics" class="space-y-4">
    <div>
        <div class="flex justify-between text-sm mb-1">
            <span>Processing Load</span>
            <span th:text="${metrics.cpuLoad} + '%'">4%</span>
        </div>
        <div class="w-full bg-gray-700 h-2 rounded-full overflow-hidden">
            <div class="bg-green-500 h-full" th:style="'width: ' + ${metrics.cpuLoad} + '%'"></div>
        </div>
    </div>
    <div>
        <div class="flex justify-between text-sm">
            <span>Gemini Latency</span>
            <span th:text="${metrics.latencyMs} + 'ms'" class="text-blue-400 font-mono">342ms</span>
        </div>
    </div>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/dashboard.html src/main/resources/templates/fragments/metrics.html
git commit -m "feat: add Thymeleaf templates"
```

---

### Task 7: Verification

- [ ] **Step 1: Run the application**

Run: `./gradlew bootRun`

- [ ] **Step 2: Access the dashboard**

Open: `http://localhost:8080/dashboard`
Verify:
1. "Registered Tools" list matches `@Tool` methods in `TorrentTools`.
2. "Processing Load" updates every 2s.
3. Logs stream in real-time to the log window.

- [ ] **Step 3: Final Commit**

```bash
git commit --allow-empty -m "docs: finalize dashboard implementation"
```
