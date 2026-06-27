package dev.naguiar.nbot.tools.socialschools

import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.Cookie
import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SocialSchoolsClient(
    private val properties: SocialSchoolsProperties,
    private val albumRepository: SocialSchoolsAlbumRepository,
) {
    private val log = LoggerFactory.getLogger(SocialSchoolsClient::class.java)
    private var authCookies: List<Cookie> = emptyList()

    fun downloadAllMedia(downloadDir: Path): List<String> {
        log.info("Starting download of all media to {}", downloadDir)
        val downloadedFiles = mutableListOf<String>()
        try {
            Playwright.create().use { playwright ->
                val browser = playwright.chromium().launch()
                val context = browser.newContext()

                if (authCookies.isNotEmpty()) {
                    log.info("Adding existing cookies to context")
                    context.addCookies(authCookies)
                }

                val page = context.newPage()
                log.info("Navigating to target URL: {}", properties.url)
                page.navigate(properties.url)

                try {
                    page.waitForSelector(
                        "#username, button[aria-label='Download']",
                        Page.WaitForSelectorOptions().setTimeout(5000.0),
                    )
                } catch (e: Exception) {
                    log.info("Timeout waiting for selector: {}", e.message)
                }

                if (page.locator("#username").count() > 0) {
                    log.info("Login page detected. Performing login flow.")
                    page.locator("#username").fill(properties.username)
                    page.locator("#Password").fill(properties.password)
                    page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Inloggen")).click()

                    log.info("Waiting for redirection back to communities...")
                    page.waitForURL("**/communities/**")
                }

                log.info("Waiting for download buttons to load...")
                page.waitForSelector("button[aria-label='Download']")

                log.info("Scrolling down to load all media cards...")
                var lastCount = 0
                var noChangeAttempts = 0
                var totalAttempts = 0
                while (totalAttempts < 30 && noChangeAttempts < 3) {
                    val currentCount = page.locator("button[aria-label='Download']").count()
                    if (currentCount == lastCount) {
                        noChangeAttempts++
                    } else {
                        noChangeAttempts = 0
                    }
                    lastCount = currentCount
                    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
                    page.waitForTimeout(2000.0)
                    totalAttempts++
                }
                log.info("Total media albums found after scrolling: {}", lastCount)

                val cards = page.locator("div.Square__Outer-sc-1xi1gsj-0")
                val count = cards.count()

                for (i in 0 until count) {
                    try {
                        val card = cards.nth(i)
                        val titleElement = card.locator(".MediaList__OverlayText-sc-14dajke-1 > div").last()
                        val titleText = titleElement.innerText().trim()

                        val (name, date) = extractNameAndDate(titleText)

                        // Check or initialize database record
                        var album = albumRepository.findByTitle(titleText)
                        if (album == null) {
                            album =
                                SocialSchoolsAlbum(
                                    title = titleText,
                                    name = name,
                                    dateText = date,
                                )
                            albumRepository.save(album)
                            log.info("Registered new album in DB: {}", titleText)
                        }

                        // Check if already downloaded and exists on disk
                        if (album.filename != null && Files.exists(downloadDir.resolve(album.filename))) {
                            log.info("Album already downloaded: {}, skipping button click.", titleText)
                            continue
                        }

                        val button = card.locator("button[aria-label='Download']")
                        val download =
                            page.waitForDownload {
                                button.click()
                            }

                        val filename = download.suggestedFilename()
                        val destination = downloadDir.resolve(filename)

                        if (Files.exists(destination)) {
                            log.info("Album already exists on disk, skipping and cancelling download: {}", filename)
                            download.cancel()

                            album =
                                album.copy(
                                    filename = filename,
                                    downloadedAt = album.downloadedAt ?: java.time.Instant.now(),
                                )
                            albumRepository.save(album)
                        } else {
                            log.info("Downloading new album: {}", filename)
                            download.saveAs(destination)
                            downloadedFiles.add(filename)

                            album =
                                album.copy(
                                    filename = filename,
                                    downloadedAt = java.time.Instant.now(),
                                )
                            albumRepository.save(album)
                        }
                    } catch (e: Exception) {
                        log.error("Failed to download album at index $i", e)
                    }
                }

                authCookies = context.cookies()
            }
        } catch (e: Exception) {
            log.error("Failed to download all media", e)
        }
        return downloadedFiles
    }

    private fun extractNameAndDate(title: String): Pair<String, String?> {
        val parts = title.split(", ")
        if (parts.size <= 1) {
            return Pair(title, null)
        }
        val rawDate = parts.last()
        val date = appendYearIfMissing(rawDate)
        val name = parts.subList(0, parts.size - 1).joinToString(", ")
        return Pair(name, date)
    }

    private fun appendYearIfMissing(dateText: String): String {
        val trimmed = dateText.trim()
        if (trimmed.firstOrNull()?.isDigit() != true) {
            return trimmed
        }
        val parts = trimmed.split(" ")
        val lastPart = parts.lastOrNull()
        if (lastPart == null || lastPart.length != 4 || lastPart.toIntOrNull() == null) {
            val currentYear =
                java.time.LocalDate
                    .now()
                    .year
            return "$trimmed $currentYear"
        }
        return trimmed
    }
}
