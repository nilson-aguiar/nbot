package dev.naguiar.nbot.tools.socialschools

import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import java.util.*

@Disabled("Manual test requiring real credentials and browser context")
class SocialSchoolsClientManualTest {
    @Test
    fun testDownloadAllAndSkipExisting() {
        val properties = Properties()
        val envFile = File(".env")
        if (envFile.exists()) {
            envFile.inputStream().use { properties.load(it) }
        }

        val username = properties.getProperty("SOCIALSCHOOLS_USERNAME") ?: ""
        val password = properties.getProperty("SOCIALSCHOOLS_PASSWORD") ?: ""
        val url = properties.getProperty("SOCIALSCHOOLS_URL") ?: "https://app.socialschools.eu/communities/242553/media"
        val loginUrl =
            properties.getProperty("SOCIALSCHOOLS_LOGIN_URL") ?: "https://login.socialschools.eu/Account/Login"

        println("Running SocialSchools download all test...")

        val albumRepository =
            object : SocialSchoolsAlbumRepository {
                private val albums = mutableMapOf<String, SocialSchoolsAlbum>()

                override fun findByTitle(title: String): SocialSchoolsAlbum? = albums[title]

                override fun findAll(): List<SocialSchoolsAlbum> = albums.values.toList()

                override fun save(album: SocialSchoolsAlbum): SocialSchoolsAlbum {
                    albums[album.title] = album
                    return album
                }

                override fun saveAll(albums: List<SocialSchoolsAlbum>): List<SocialSchoolsAlbum> {
                    albums.forEach { save(it) }
                    return albums
                }

                override fun deleteAll() {
                    albums.clear()
                }
            }

        val client =
            SocialSchoolsClient(
                SocialSchoolsProperties(
                    username = username,
                    password = password,
                    url = url,
                    loginUrl = loginUrl,
                ),
                albumRepository,
            )

        val tmpPath = Paths.get("tmp")

        // First run: should download files
        println("\n--- RUN 1: Downloading all files ---")
        val downloadedFirst = client.downloadAllMedia(tmpPath)
        println("Downloaded in Run 1: $downloadedFirst")

        // Second run: should identify files already exist and skip them
        println("\n--- RUN 2: Checking skip existing functionality ---")
        val downloadedSecond = client.downloadAllMedia(tmpPath)
        println("Downloaded in Run 2 (should be empty): $downloadedSecond")

        assert(
            downloadedSecond.isEmpty(),
        ) { "Failed: Some files were downloaded again even though they already exist!" }
        println("\nSuccess: Skip-existing works perfectly!")
    }
}
