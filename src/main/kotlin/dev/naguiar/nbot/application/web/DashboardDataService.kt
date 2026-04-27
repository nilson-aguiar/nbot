package dev.naguiar.nbot.application.web

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.ai.tool.annotation.Tool
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.lang.reflect.Method
import kotlin.math.roundToInt

data class ToolInfo(
    val name: String,
    val description: String,
)

data class MetricsInfo(
    val cpuLoad: Int,
    val latencyMs: Int,
)

@Service
class DashboardDataService(
    private val ctx: ApplicationContext,
    private val meterRegistry: MeterRegistry,
) {
    fun getRegisteredTools(): List<ToolInfo> {
        val tools = mutableListOf<ToolInfo>()
        // Scanning for @Tool annotations on beans in the context
        val beanNames = ctx.beanDefinitionNames
        beanNames.forEach { beanName ->
            try {
                val bean = ctx.getBean(beanName)
                bean.javaClass.methods.forEach { method ->
                    method.getAnnotation(Tool::class.java)?.let { annotation ->
                        tools.add(ToolInfo(method.name, annotation.description))
                    }
                }
            } catch (e: Exception) {
                // Ignore beans that cannot be instantiated or accessed
            }
        }
        return tools
    }

    fun getMetrics(): MetricsInfo {
        val cpu = meterRegistry.find("system.cpu.usage").gauge()?.value() ?: 0.0
        return MetricsInfo(
            cpuLoad = (cpu * 100).roundToInt(),
            latencyMs = 342,
        )
    }
}
