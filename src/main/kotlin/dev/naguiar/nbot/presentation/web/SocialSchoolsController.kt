package dev.naguiar.nbot.presentation.web

import dev.naguiar.nbot.infrastructure.config.SocialSchoolsProperties
import dev.naguiar.nbot.tools.socialschools.SocialSchoolsAlbumRepository
import dev.naguiar.nbot.tools.socialschools.SocialSchoolsClient
import java.nio.file.Paths
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/dashboard/socialschools")
@Controller
class SocialSchoolsController(
    private val client: SocialSchoolsClient,
    private val albumRepository: SocialSchoolsAlbumRepository,
    properties: SocialSchoolsProperties,
) {
    private val downloadDir =
        properties.downloadDir?.let { Paths.get(it) }
            ?: Paths.get(System.getProperty("user.home"), "socialschools")

    @Suppress("SpringMvcViewInspection")
    @GetMapping
    fun socialSchoolsFragment(model: Model): String {
        val albums =
            albumRepository
                .findAll()
                .sortedWith(
                    compareByDescending<dev.naguiar.nbot.tools.socialschools.SocialSchoolsAlbum> {
                        getSortableDate(
                            it.dateText,
                        )
                    }.thenByDescending { it.title },
                )
        model.addAttribute("albums", albums)
        return "fragments/socialschools :: socialschools"
    }

    private fun getSortableDate(dateText: String?): java.time.LocalDate {
        if (dateText == null) return java.time.LocalDate.MIN
        try {
            val parts = dateText.trim().split(" ")
            if (parts.size >= 2) {
                val day = parts[0].toIntOrNull()
                val monthStr = parts[1].lowercase()
                val month =
                    when {
                        monthStr.startsWith("jan") -> 1
                        monthStr.startsWith("feb") -> 2
                        monthStr.startsWith("mar") || monthStr.startsWith("mrt") -> 3
                        monthStr.startsWith("apr") -> 4
                        monthStr.startsWith("may") || monthStr.startsWith("mei") -> 5
                        monthStr.startsWith("jun") -> 6
                        monthStr.startsWith("jul") -> 7
                        monthStr.startsWith("aug") -> 8
                        monthStr.startsWith("sep") -> 9
                        monthStr.startsWith("oct") || monthStr.startsWith("okt") -> 10
                        monthStr.startsWith("nov") -> 11
                        monthStr.startsWith("dec") -> 12
                        else -> 1
                    }
                val currentYear =
                    java.time.LocalDate
                        .now()
                        .year
                val year = if (parts.size >= 3) parts[2].toIntOrNull() ?: currentYear else currentYear
                if (day != null) {
                    return java.time.LocalDate.of(year, month, day)
                }
            }
        } catch (_: Exception) {
            // Ignore
        }
        return java.time.LocalDate.MIN
    }

    @PostMapping("/check")
    fun checkNewAlbums(model: Model): String {
        // Trigger check/download using Playwright client
        client.downloadAllMedia(downloadDir)
        return socialSchoolsFragment(model)
    }

    @PostMapping("/clear")
    fun clearData(model: Model): String {
        albumRepository.deleteAll()
        return socialSchoolsFragment(model)
    }

    @GetMapping("/files/{filename}")
    fun downloadFile(
        @PathVariable filename: String,
    ): ResponseEntity<Resource> {
        val cleanPath = downloadDir.resolve(filename).normalize()
        if (!cleanPath.startsWith(downloadDir)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build()
        }
        val file = cleanPath.toFile()
        if (!file.exists()) {
            return ResponseEntity.notFound().build()
        }
        val resource = FileSystemResource(file)
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(resource)
    }
}
