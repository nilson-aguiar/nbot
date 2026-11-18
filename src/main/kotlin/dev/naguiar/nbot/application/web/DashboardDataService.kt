package dev.naguiar.nbot.application.web

import dev.naguiar.nbot.tools.BotTool
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

data class ToolInfo(
    val name: String,
    val description: String,
)

@Service
class DashboardDataService(
    private val ctx: ApplicationContext,
) {
    fun getRegisteredTools(): List<ToolInfo> {
        val tools = mutableListOf<ToolInfo>()
        // Scanning for @BotTool annotations on beans in the context
        val beanNames = ctx.beanDefinitionNames
        beanNames.forEach { beanName ->
            try {
                val bean = ctx.getBean(beanName)
                bean.javaClass.methods.forEach { method ->
                    method.getAnnotation(BotTool::class.java)?.let { annotation ->
                        tools.add(ToolInfo(method.name, annotation.description))
                    }
                }
            } catch (_: Exception) {
                // Ignore beans that cannot be instantiated or accessed
            }
        }
        return tools
    }
}
