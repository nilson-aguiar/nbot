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
        
        verify(exactly = 1) { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { emitter2.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `should remove dead emitters on broadcast failure`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        every { emitter.send(any<SseEmitter.SseEventBuilder>()) } throws Exception("Dead")
        
        service.addEmitter(emitter)
        service.broadcast("test")
        
        // Second broadcast should not attempt to send to the dead emitter
        service.broadcast("test2")
        
        verify(exactly = 1) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }
}
