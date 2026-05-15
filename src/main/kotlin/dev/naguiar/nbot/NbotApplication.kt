package dev.naguiar.nbot

import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(ActualBudgetProperties::class)
@EnableAsync
class NbotApplication

fun main(args: Array<String>) {
    runApplication<NbotApplication>(*args)
}
