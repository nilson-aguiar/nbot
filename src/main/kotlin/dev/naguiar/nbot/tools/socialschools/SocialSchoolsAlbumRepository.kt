package dev.naguiar.nbot.tools.socialschools

interface SocialSchoolsAlbumRepository {
    fun findByTitle(title: String): SocialSchoolsAlbum?

    fun findAll(): List<SocialSchoolsAlbum>

    fun save(album: SocialSchoolsAlbum): SocialSchoolsAlbum

    fun saveAll(albums: List<SocialSchoolsAlbum>): List<SocialSchoolsAlbum>

    fun deleteAll()
}
