package dev.naguiar.nbot.presentation.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController {
    @GetMapping("/")
    fun index(): String = "redirect:/dashboard"
}
