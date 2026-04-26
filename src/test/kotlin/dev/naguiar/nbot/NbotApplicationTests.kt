package dev.naguiar.nbot

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("ollama")
class NbotApplicationTests {
    @Test
    fun contextLoads() {
    }
}
