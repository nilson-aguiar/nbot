package dev.naguiar.nbot.tools.socialschools

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("manual-test")
class SocialSchoolsTestController(
    private val socialSchoolsService: SocialSchoolsService,
) {
    @PostMapping("/test/social-schools/download")
    fun triggerDownload(): ResponseEntity<String> {
        socialSchoolsService.downloadNewFiles()
        return ResponseEntity.ok("Download task triggered. Check the logs for details.")
    }
}
