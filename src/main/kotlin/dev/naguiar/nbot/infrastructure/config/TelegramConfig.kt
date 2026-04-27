package dev.naguiar.nbot.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Configuration
class TelegramConfig {
    @Bean
    fun telegramBotsApi(bots: List<LongPollingBot>): TelegramBotsApi {
        val api = TelegramBotsApi(DefaultBotSession::class.java)
        bots.forEach { api.registerBot(it) }
        return api
    }
}
