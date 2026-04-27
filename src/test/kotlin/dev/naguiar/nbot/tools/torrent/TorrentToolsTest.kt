package dev.naguiar.nbot.tools.torrent

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TorrentToolsTest {
    @MockK
    private lateinit var qBittorrentClient: QBittorrentClient

    @InjectMockKs
    private lateinit var torrentTools: TorrentTools

    @Test
    fun `addTorrentMagnet should return success message when client returns true`() {
        val magnetLink = "magnet:?xt=urn:btih:d12134..."
        every { qBittorrentClient.addMagnetLink(magnetLink) } returns true

        val result = torrentTools.addTorrentMagnet(magnetLink)

        assertThat(result).isEqualTo("Successfully added magnet link to qBittorrent.")
    }

    @Test
    fun `addTorrentMagnet should return failure message when client returns false`() {
        val magnetLink = "magnet:?xt=urn:btih:invalid"
        every { qBittorrentClient.addMagnetLink(magnetLink) } returns false

        val result = torrentTools.addTorrentMagnet(magnetLink)

        assertThat(result).isEqualTo("Failed to add magnet link to qBittorrent.")
    }

    @Test
    fun `addTorrentFile should return success message when file exists and client returns true`() {
        val tempFile = java.io.File.createTempFile("test", ".torrent")
        try {
            every { qBittorrentClient.addTorrentFile(tempFile) } returns true

            val result = torrentTools.addTorrentFile(tempFile.absolutePath)

            assertThat(result).isEqualTo("Successfully added the torrent file to qBittorrent.")
            assertThat(tempFile.exists()).isFalse()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `addTorrentFile should return failure message when file exists but client returns false`() {
        val tempFile = java.io.File.createTempFile("test", ".torrent")
        try {
            every { qBittorrentClient.addTorrentFile(tempFile) } returns false

            val result = torrentTools.addTorrentFile(tempFile.absolutePath)

            assertThat(result).isEqualTo("Failed to add the torrent file to qBittorrent.")
            assertThat(tempFile.exists()).isFalse()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `addTorrentFile should return file not found when file does not exist`() {
        val result = torrentTools.addTorrentFile("/non/existent/file.torrent")
        assertThat(result).isEqualTo("File not found.")
    }
}
