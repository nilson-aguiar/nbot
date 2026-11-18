package dev.naguiar.nbot.tools

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BotTool(
    val description: String,
)
