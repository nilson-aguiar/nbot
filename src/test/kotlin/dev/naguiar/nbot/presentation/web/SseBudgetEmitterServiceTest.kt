package dev.naguiar.nbot.presentation.web

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

class SseBudgetEmitterServiceTest {
    @Test
    fun `should add emitter and broadcast messages`() {
        val service = SseBudgetEmitterService()
        val emitter = mockk<SseEmitter>(relaxed = true)

        service.addEmitter(emitter)
        service.broadcast("<p>test</p>")

        verify(exactly = 1) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `should remove dead emitters on error`() {
        val service = SseBudgetEmitterService()
        val deadEmitter = mockk<SseEmitter>(relaxed = true)
        val goodEmitter = mockk<SseEmitter>(relaxed = true)

        every { deadEmitter.send(any<SseEmitter.SseEventBuilder>()) } throws IOException("Client disconnected")

        service.addEmitter(deadEmitter)
        service.addEmitter(goodEmitter)

        service.broadcast("<p>test</p>")

        verify(exactly = 1) { deadEmitter.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { goodEmitter.send(any<SseEmitter.SseEventBuilder>()) }

        // Broadcast again to verify the dead emitter was removed
        service.broadcast("<p>test2</p>")

        verify(exactly = 1) { deadEmitter.send(any<SseEmitter.SseEventBuilder>()) } // No new interaction
        verify(exactly = 2) { goodEmitter.send(any<SseEmitter.SseEventBuilder>()) } // One new interaction
    }
}
