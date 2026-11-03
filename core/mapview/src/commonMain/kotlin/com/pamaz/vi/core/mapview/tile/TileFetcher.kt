package com.pamaz.vi.core.mapview.tile

import androidx.compose.ui.graphics.ImageBitmap
import com.pamaz.vi.core.mapview.decodeImageBitmapDownscaled
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Where a successfully fetched tile's bytes actually came from — feeds [TileCacheMetrics] directly. */
internal enum class TileDataSource { DISK, NETWORK }

internal sealed interface TileFetchResult {
    data class Success(val bitmap: ImageBitmap, val byteSize: Long, val source: TileDataSource) :
        TileFetchResult

    data class Failure(val error: Throwable) : TileFetchResult
}

/**
 * Fetches and decodes one tile's bytes. [TileLoadCoordinator] is the only caller — this interface
 * exists so the coordinator's dedup/priority/retry logic can be unit-tested against a fake
 * (controllable delay, failure, call-counting) with no real network or platform decoder involved.
 */
internal interface TileFetcher {
    suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult
}

/**
 * The real [TileFetcher]: [diskCache] first (a hit never needs revalidation, see its own KDoc),
 * then [httpClient], writing a network hit back to [diskCache] for next time. Decoding reuses
 * [decodeImageBitmapDownscaled] — already proven correct per-platform (Android `BitmapFactory`,
 * JVM/iOS Skia) by [com.pamaz.vi.core.mapview.DevMapImage] — with [tileSizePx] as the cap: a tile
 * is never larger than that on either axis, so this is a decode, not an actual downscale, for a
 * well-formed tile; it only kicks in defensively if a corrupt/oversized file ever made it past
 * generation.
 */
internal class HttpTileFetcher(
    private val httpClient: HttpClient,
    private val diskCache: TileDiskCache,
) : TileFetcher {
    override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult = try {
        val cachedBytes = diskCache.read(url)
        if (cachedBytes != null) {
            val decoded = decodeImageBitmapDownscaled(cachedBytes, tileSizePx)
            TileFetchResult.Success(decoded.bitmap, cachedBytes.size.toLong(), TileDataSource.DISK)
        } else {
            val response: HttpResponse = httpClient.get(url)
            if (!response.status.isSuccess()) {
                TileFetchResult.Failure(
                    IllegalStateException("HTTP ${response.status.value} fetching $url")
                )
            } else {
                val bytes = response.body<ByteArray>()
                diskCache.write(url, bytes)
                val decoded = decodeImageBitmapDownscaled(bytes, tileSizePx)
                TileFetchResult.Success(decoded.bitmap, bytes.size.toLong(), TileDataSource.NETWORK)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TileFetchResult.Failure(e)
    }
}
