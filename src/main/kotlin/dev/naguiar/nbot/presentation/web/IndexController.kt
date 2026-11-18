package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.application.web.HomepageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

@Controller
class IndexController(
    private val homepageService: HomepageService,
) {
    @GetMapping("/")
    fun index(
        request: HttpServletRequest,
        model: Model,
    ): String {
        val requestHost = resolveRequestHost(request)
        model.addAttribute("homepage", homepageService.getHomepageData(requestHost))
        return "homepage"
    }

    @PostMapping("/homepage/refresh")
    fun refresh(
        request: HttpServletRequest,
        model: Model,
    ): String {
        val requestHost = resolveRequestHost(request)
        model.addAttribute("homepage", homepageService.getHomepageData(requestHost, forceRefresh = true))
        return "fragments/homepage-services :: services"
    }

    private fun resolveRequestHost(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-Host")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.serverName
}
