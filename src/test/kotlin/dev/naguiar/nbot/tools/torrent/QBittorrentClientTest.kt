package dev.naguiar.nbot.tools.torrent

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*

@RestClientTest(QBittorrentClient::class)
@Import(QBittorrentProperties::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QBittorrentClientTest {

    @Autowired
    private lateinit var client: QBittorrentClient

    @Autowired
    private lateinit var server: MockRestServiceServer

    @Autowired
    private lateinit var properties: QBittorrentProperties

    @Test
    fun `addMagnetLink should login and return true on success`() {
        properties.url = "http://localhost:8080"
        properties.username = "admin"
        properties.password = "adminadmin"

        // Mock login
        server.expect(requestTo("http://localhost:8080/api/v2/auth/login"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andRespond(withSuccess().header(HttpHeaders.SET_COOKIE, "SID=12345; HttpOnly"))

        // Mock add torrent
        server.expect(requestTo("http://localhost:8080/api/v2/torrents/add"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.COOKIE, "SID=12345"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andRespond(withSuccess())

        val result = client.addMagnetLink("magnet:?xt=urn:btih:fake")

        assertThat(result).isTrue()
        server.verify()
    }
    
    @Test
    fun `addMagnetLink should return false on server error after retry`() {
        properties.url = "http://localhost:8080"
        properties.username = "admin"
        properties.password = "adminadmin"

        // Mock login (first attempt)
        server.expect(requestTo("http://localhost:8080/api/v2/auth/login"))
            .andRespond(withSuccess().header(HttpHeaders.SET_COOKIE, "SID=12345; HttpOnly"))

        // Mock add torrent (fails first time)
        server.expect(requestTo("http://localhost:8080/api/v2/torrents/add"))
            .andRespond(withServerError())

        // Client attempts re-login due to failure cache break fallback
        server.expect(requestTo("http://localhost:8080/api/v2/auth/login"))
            .andRespond(withSuccess().header(HttpHeaders.SET_COOKIE, "SID=67890; HttpOnly"))

        // Client attempts add torrent again (Mock fails again)
        server.expect(requestTo("http://localhost:8080/api/v2/torrents/add"))
            .andRespond(withServerError())

        val result = client.addMagnetLink("magnet:?xt=urn:btih:fake")

        assertThat(result).isFalse()
        server.verify()
    }

    @Test
    fun `addTorrentFile should upload multipart file and return true on success`() {
        properties.url = "http://localhost:8080"
        properties.username = "admin"
        properties.password = "adminadmin"

        val tempFile = java.io.File.createTempFile("test", ".torrent")
        tempFile.writeText("dummy content")

        // Mock login
        server.expect(requestTo("http://localhost:8080/api/v2/auth/login"))
            .andRespond(withSuccess().header(HttpHeaders.SET_COOKIE, "SID=12345; HttpOnly"))

        // Mock add torrent
        server.expect(requestTo("http://localhost:8080/api/v2/torrents/add"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.COOKIE, "SID=12345"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
            .andRespond(withSuccess())

        val result = client.addTorrentFile(tempFile)

        assertThat(result).isTrue()
        server.verify()
        tempFile.delete()
    }
}
