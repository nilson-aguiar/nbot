package dev.naguiar.nbot.tools.socialschools

import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Paths

@ExtendWith(MockKExtension::class)
class SocialSchoolsServiceTest {
    @MockK
    private lateinit var client: SocialSchoolsClient

    @MockK
    private lateinit var properties: SocialSchoolsProperties

    private lateinit var service: SocialSchoolsService

    private val downloadDir = Paths.get(System.getProperty("user.home"), "socialschools")

    @BeforeEach
    fun setUp() {
        every { properties.downloadDir } returns null
        service = SocialSchoolsService(client, properties)
    }

    @Test
    fun `downloadNewFiles should invoke client downloadAllMedia`() {
        // Given
        every { client.downloadAllMedia(downloadDir) } returns listOf("file1.zip", "file2.zip")

        // When
        service.downloadNewFiles()

        // Then
        verify(exactly = 1) { client.downloadAllMedia(downloadDir) }
    }
}
