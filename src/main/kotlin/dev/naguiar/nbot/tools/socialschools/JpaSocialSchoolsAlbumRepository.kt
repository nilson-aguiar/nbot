package dev.naguiar.nbot.tools.socialschools

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaSocialSchoolsAlbumRepository : JpaRepository<SocialSchoolsAlbumEntity, String>
