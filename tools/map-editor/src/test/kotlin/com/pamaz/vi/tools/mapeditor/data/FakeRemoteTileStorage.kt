package com.pamaz.vi.tools.mapeditor.data

import io.ktor.http.ContentType

/**
 * Test double for [RemoteTileStorage] — tracks how many times [upload] was called per path (so a
 * test can assert a retry doesn't re-upload a path it already succeeded on) and can be told to
 * fail specific paths on demand, without any network call.
 */
class FakeRemoteTileStorage : RemoteTileStorage {
    val failPaths = mutableSetOf<String>()
    val uploadCallCounts = mutableMapOf<String, Int>()
    private val uploaded = mutableSetOf<String>()

    override suspend fun exists(path: String): Boolean = path in uploaded

    override suspend fun upload(path: String, bytes: ByteArray, contentType: ContentType) {
        uploadCallCounts[path] = (uploadCallCounts[path] ?: 0) + 1
        if (path in failPaths) error("simulated upload failure for $path")
        uploaded += path
    }

    override fun publicUrl(path: String): String =
        "https://example.supabase.co/storage/v1/object/public/map-tiles/$path"
}
