package dev.naguiar.nbot.tools.socialschools

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SocialSchoolsAlbum(
    val title: String,
    val name: String,
    val dateText: String?,
    val filename: String? = null,
    val downloadedAt: Instant? = null,
) {
    @Suppress("unused")
    val formattedDownloadedAt: String?
        get() =
            downloadedAt?.let {
                DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(it)
            }

    @Suppress("unused")
    val formattedDate: String
        get() {
            if (dateText == null) return "Unknown"
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
                    val currentYear = LocalDate.now().year
                    val year = if (parts.size >= 3) parts[2].toIntOrNull() ?: currentYear else currentYear
                    if (day != null) {
                        val localDate = LocalDate.of(year, month, day)
                        return localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    }
                }
            } catch (_: Exception) {
                // Fallback
            }
            return dateText
        }
}
