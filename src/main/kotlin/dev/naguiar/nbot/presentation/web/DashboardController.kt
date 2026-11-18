package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.DashboardDataService
import dev.naguiar.nbot.application.web.HomepageService
import dev.naguiar.nbot.infrastructure.logging.SseLogEmitterService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RequestMapping("/dashboard")
@Controller
class DashboardController(
    private val dataService: DashboardDataService,
    private val homepageService: HomepageService,
    private val logEmitterService: SseLogEmitterService,
) {
    @GetMapping
    fun dashboard(
        request: HttpServletRequest,
        model: Model,
    ): String {
        val requestHost = resolveRequestHost(request)
        model.addAttribute("tools", dataService.getRegisteredTools())
        model.addAttribute("homepage", homepageService.getHomepageData(requestHost))
        return "dashboard"
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
