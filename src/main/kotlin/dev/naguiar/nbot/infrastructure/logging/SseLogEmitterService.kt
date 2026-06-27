package dev.naguiar.nbot.infrastructure.logging

import java.util.concurrent.CopyOnWriteArrayList
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

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
            } catch (_: Exception) {
                deadEmitters.add(emitter)
            }
        }
        emitters.removeAll(deadEmitters)
    }
}
