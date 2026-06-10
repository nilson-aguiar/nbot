package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RequestMapping("/dashboard")
@Controller
class DashboardController(
    private val dataService: DashboardDataService,
    private val logEmitterService: SseLogEmitterService,
) {
    @GetMapping
    fun dashboard(model: Model): String {
        model.addAttribute("tools", dataService.getRegisteredTools())
        model.addAttribute("metrics", dataService.getMetrics())
        return "dashboard"
    }

    @GetMapping("/metrics")
    fun metricsFragment(model: Model): String {
        model.addAttribute("metrics", dataService.getMetrics())
        return "fragments/metrics :: metrics"
    }

    @GetMapping("/torrents")
    fun torrentsFragment(): String = "fragments/logs :: logs"

    @GetMapping("/logs/stream")
    fun logStream(): SseEmitter {
        val emitter = SseEmitter(-1L)
        logEmitterService.addEmitter(emitter)
        return emitter
    }
}
