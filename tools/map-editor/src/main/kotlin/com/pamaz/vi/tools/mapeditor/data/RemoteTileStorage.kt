package com.pamaz.vi.tools.mapeditor.data

import io.ktor.http.ContentType

/**
 * The narrow slice of Supabase Storage PLT-08's upload step actually needs — deliberately not
 * supabase-kt's full `BucketApi` (20+ methods: signed URLs, resumable uploads, image transforms,
 * ...), so a test fake only has 3 methods to implement instead of stubbing everything else out.
 */
interface RemoteTileStorage {
    /** Used by [com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapPublisher] to skip a tile a previous, failed attempt already uploaded. */
    suspend fun exists(path: String): Boolean

    suspend fun upload(path: String, bytes: ByteArray, contentType: ContentType)

    fun publicUrl(path: String): String
}
