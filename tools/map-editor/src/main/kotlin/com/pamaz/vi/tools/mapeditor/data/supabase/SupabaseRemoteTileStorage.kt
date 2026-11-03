package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.tools.mapeditor.data.RemoteTileStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

internal const val MAP_TILES_BUCKET = "map-tiles"

/** Thin [RemoteTileStorage] wrapper over the real `map-tiles` bucket, through the signed-in admin's own session (never the service-role key). */
class SupabaseRemoteTileStorage(client: SupabaseClient) : RemoteTileStorage {
    private val bucket = client.storage.from(MAP_TILES_BUCKET)

    override suspend fun exists(path: String): Boolean = bucket.exists(path)

    override suspend fun upload(path: String, bytes: ByteArray, contentType: ContentType) {
        bucket.upload(path, bytes) {
            upsert = true
            this.contentType = contentType
            // `UploadOptionBuilder` has no first-class `cacheControl` property (verified via
            // javap against the real 3.1.4 jar, not assumed) — `httpOverride` is its documented
            // escape hatch for exactly this. Each map version has its own folder and never
            // changes in place (see `SupabaseAdminClient.uploadTiles`'s matching KDoc in
            // `:tools:tilegen`), so a client never needs to revalidate a cached tile.
            httpOverride { header(HttpHeaders.CacheControl, "max-age=31536000, immutable") }
        }
    }

    override fun publicUrl(path: String): String = bucket.publicUrl(path)
}
