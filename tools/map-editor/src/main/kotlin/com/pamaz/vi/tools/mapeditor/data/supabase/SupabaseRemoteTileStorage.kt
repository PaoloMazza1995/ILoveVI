package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.tools.mapeditor.data.RemoteTileStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType

internal const val MAP_TILES_BUCKET = "map-tiles"

/** Thin [RemoteTileStorage] wrapper over the real `map-tiles` bucket, through the signed-in admin's own session (never the service-role key). */
class SupabaseRemoteTileStorage(client: SupabaseClient) : RemoteTileStorage {
    private val bucket = client.storage.from(MAP_TILES_BUCKET)

    override suspend fun exists(path: String): Boolean = bucket.exists(path)

    override suspend fun upload(path: String, bytes: ByteArray, contentType: ContentType) {
        bucket.upload(path, bytes) {
            upsert = true
            this.contentType = contentType
        }
    }

    override fun publicUrl(path: String): String = bucket.publicUrl(path)
}
