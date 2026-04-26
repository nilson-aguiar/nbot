package dev.naguiar.nbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class NbotApplication

fun main(args: Array<String>) {
    runApplication<NbotApplication>(*args)
}
