package dev.naguiar.nbot.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext

class SseLogbackAppenderTest {
    private val applicationContext = mockk<ApplicationContext>()
    private val emitterService = mockk<SseLogEmitterService>(relaxed = true)
    private lateinit var appender: SseLogbackAppender

    @BeforeEach
    fun setUp() {
        appender = SseLogbackAppender()
        appender.start()
    }

    @Test
    fun `should not append if application context is not initialized`() {
        val event = mockk<ILoggingEvent>()

        // Context not set
        appender.doAppend(event)

        verify(exactly = 0) { emitterService.broadcast(any()) }
    }

    @Test
    fun `should append ERROR log with red color`() {
        appender.setApplicationContext(applicationContext)
        every { applicationContext.getBean(SseLogEmitterService::class.java) } returns emitterService

        val event = mockk<ILoggingEvent>()
        every { event.level } returns Level.ERROR
        every { event.timeStamp } returns System.currentTimeMillis()
        every { event.formattedMessage } returns "Error message"

        appender.doAppend(event)

        verify {
            emitterService.broadcast(
                match {
                    it.contains("text-red-500") &&
                        it.contains("Error message") &&
                        it.contains("[ERROR]")
                },
            )
        }
    }

    @Test
    fun `should append WARN log with yellow color`() {
        appender.setApplicationContext(applicationContext)
        every { applicationContext.getBean(SseLogEmitterService::class.java) } returns emitterService

        val event = mockk<ILoggingEvent>()
        every { event.level } returns Level.WARN
        every { event.timeStamp } returns System.currentTimeMillis()
        every { event.formattedMessage } returns "Warning message"

        appender.doAppend(event)

        verify {
            emitterService.broadcast(
                match { it.contains("text-yellow-500") && it.contains("Warning message") && it.contains("[WARN]") },
            )
        }
    }

    @Test
    fun `should append INFO log with green color`() {
        appender.setApplicationContext(applicationContext)
        every { applicationContext.getBean(SseLogEmitterService::class.java) } returns emitterService

        val event = mockk<ILoggingEvent>()
        every { event.level } returns Level.INFO
        every { event.timeStamp } returns System.currentTimeMillis()
        every { event.formattedMessage } returns "Info message"

        appender.doAppend(event)

        verify {
            emitterService.broadcast(
                match {
                    it.contains("text-green-500") &&
                        it.contains("Info message") &&
                        it.contains("[INFO]")
                },
            )
        }
    }
}
