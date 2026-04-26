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
