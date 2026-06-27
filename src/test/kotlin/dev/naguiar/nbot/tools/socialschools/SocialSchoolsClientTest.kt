package dev.naguiar.nbot.tools.socialschools

import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SocialSchoolsClientTest {
    @MockK
    private lateinit var properties: SocialSchoolsProperties

    @MockK
    private lateinit var albumRepository: SocialSchoolsAlbumRepository

    private lateinit var client: SocialSchoolsClient

    @BeforeEach
    fun setUp() {
        client = spyk(SocialSchoolsClient(properties, albumRepository))
    }

    @Test
    fun `client should be instantiated`() {
        assertThat(client).isNotNull()
    }
}
