package dev.naguiar.nbot.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Configuration
@ConditionalOnProperty(name = ["nbot.telegram.enabled"], havingValue = "true")
class TelegramConfig {

    @Bean
    fun telegramBotsApi(bots: List<LongPollingBot>): TelegramBotsApi {
        val api = TelegramBotsApi(DefaultBotSession::class.java)
        bots.forEach { api.registerBot(it) }
        return api
    }

}
