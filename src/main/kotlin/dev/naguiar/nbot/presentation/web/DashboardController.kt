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
