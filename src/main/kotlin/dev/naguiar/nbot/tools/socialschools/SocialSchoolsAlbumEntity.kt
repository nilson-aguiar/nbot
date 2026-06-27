package dev.naguiar.nbot.tools.socialschools

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "social_schools_album")
class SocialSchoolsAlbumEntity(
    @Id
    @Column(name = "title", nullable = false)
    var title: String = "",
    @Column(name = "name", nullable = false)
    var name: String = "",
    @Column(name = "date_text")
    var dateText: String? = null,
    @Column(name = "filename")
    var filename: String? = null,
    @Column(name = "downloaded_at")
    var downloadedAt: Instant? = null,
) {
    fun toDomain() =
        SocialSchoolsAlbum(
            title = title,
            name = name,
            dateText = dateText,
            filename = filename,
            downloadedAt = downloadedAt,
        )

    companion object {
        fun fromDomain(domain: SocialSchoolsAlbum) =
            SocialSchoolsAlbumEntity(
                title = domain.title,
                name = domain.name,
                dateText = domain.dateText,
                filename = domain.filename,
                downloadedAt = domain.downloadedAt,
            )
    }
}
