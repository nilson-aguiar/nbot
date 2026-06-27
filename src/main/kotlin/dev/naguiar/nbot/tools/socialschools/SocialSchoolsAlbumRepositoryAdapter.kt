package dev.naguiar.nbot.tools.socialschools

import org.springframework.stereotype.Component

@Component
class SocialSchoolsAlbumRepositoryAdapter(
    private val jpaRepository: JpaSocialSchoolsAlbumRepository,
) : SocialSchoolsAlbumRepository {
    override fun findByTitle(title: String): SocialSchoolsAlbum? =
        jpaRepository.findById(title).orElse(null)?.toDomain()

    override fun findAll(): List<SocialSchoolsAlbum> = jpaRepository.findAll().map { it.toDomain() }

    override fun save(album: SocialSchoolsAlbum): SocialSchoolsAlbum {
        val entity = SocialSchoolsAlbumEntity.fromDomain(album)
        return jpaRepository.save(entity).toDomain()
    }

    override fun saveAll(albums: List<SocialSchoolsAlbum>): List<SocialSchoolsAlbum> {
        val entities = albums.map { SocialSchoolsAlbumEntity.fromDomain(it) }
        return jpaRepository.saveAll(entities).map { it.toDomain() }
    }

    override fun deleteAll() {
        jpaRepository.deleteAll()
    }
}
