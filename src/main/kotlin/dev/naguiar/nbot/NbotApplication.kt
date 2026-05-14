package dev.naguiar.nbot

import dev.naguiar.nbot.budget.infrastructure.config.ActualBudgetProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(ActualBudgetProperties::class)
class NbotApplication

fun main(args: Array<String>) {
    runApplication<NbotApplication>(*args)
}
