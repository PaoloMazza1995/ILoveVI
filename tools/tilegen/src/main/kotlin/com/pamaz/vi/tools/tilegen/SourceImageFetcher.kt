package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.tools.tilegen.SourceImageFetcher.MAX_DOWNLOAD_BYTES
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.outputStream

/**
 * Downloads a [TileSource.Remote] image once into a temp folder before tiling.
 *
 * Rights note: this project never bundles or re-hosts an official or fan-made game map. Whatever
 * URL is passed in must point at an image the project owns or is otherwise allowed to use — for
 * local development, generate one with [com.pamaz.vi.tools.tilegen.placeholder.PlaceholderMapImage]
 * instead of pointing this at a real map.
 */
object SourceImageFetcher {
    /**
     * Generous enough for a 16k source map at full bit depth, but still a hard ceiling: without
     * one, a misconfigured or malicious URL could download an unbounded amount of data onto disk.
     */
    private const val MAX_DOWNLOAD_BYTES = 300L * 1024 * 1024

    /**
     * Downloads [url] into [tempDir] and returns the local path, or throws
     * [IllegalArgumentException] with a specific reason: not HTTPS, a non-2xx response, a
     * content type whose top-level MIME type isn't `image`, or exceeding [MAX_DOWNLOAD_BYTES].
     */
    suspend fun fetch(url: String, tempDir: Path, httpClient: HttpClient): Path {
        require(url.startsWith("https://")) {
            "source URL must use HTTPS, was: $url"
        }

        val response: HttpResponse = httpClient.get(url)
        require(response.status.isSuccess()) {
            "failed to download $url: HTTP ${response.status.value}"
        }

        val contentType = response.contentType()
        require(contentType != null && contentType.contentType == "image") {
            "expected an image response from $url, got content-type ${contentType ?: "<none>"}"
        }

        val destination = tempDir.resolve("source-image${extensionFor(contentType)}")
        val channel = response.bodyAsChannel()
        var bytesWritten = 0L
        destination.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = channel.readAvailable(buffer)
                if (read == -1) break
                if (read == 0) continue
                bytesWritten += read
                require(bytesWritten <= MAX_DOWNLOAD_BYTES) {
                    "download from $url exceeded the $MAX_DOWNLOAD_BYTES byte limit"
                }
                out.write(buffer, 0, read)
            }
        }
        return destination
    }

    private fun extensionFor(contentType: ContentType): String =
        when (contentType.contentSubtype.lowercase()) {
            "jpeg", "jpg" -> ".jpg"
            "webp" -> ".webp"
            else -> ".png"
        }
}

internal fun createTempDownloadDir(): Path = Files.createTempDirectory("tilegen-source-")
