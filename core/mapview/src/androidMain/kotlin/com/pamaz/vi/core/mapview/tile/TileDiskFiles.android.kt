package com.pamaz.vi.core.mapview.tile

import java.io.File

internal actual fun createTileDiskFiles(directoryPath: String): TileDiskFiles =
    FileBackedTileDiskFiles(directoryPath)

/** Shared almost verbatim with the JVM/desktop actual (both are plain `java.io.File`); kept as two small per-target files rather than a custom intermediate source set, matching this module's existing `ImageDecoding.jvm.kt`/`.ios.kt` precedent. */
internal class FileBackedTileDiskFiles(directoryPath: String) : TileDiskFiles {
    private val dir = File(directoryPath).apply { mkdirs() }

    override fun read(fileName: String): ByteArray? {
        val file = File(dir, fileName)
        return if (file.isFile) file.readBytes() else null
    }

    override fun write(fileName: String, bytes: ByteArray) {
        File(dir, fileName).writeBytes(bytes)
    }

    override fun delete(fileName: String) {
        File(dir, fileName).delete()
    }

    override fun list(): List<TileDiskFileInfo> =
        dir.listFiles()?.filter { it.isFile }?.map {
            TileDiskFileInfo(it.name, it.length(), it.lastModified())
        } ?: emptyList()
}

/** [android.content.Context.getCacheDir] — cleared by the OS under storage pressure on its own, which is exactly the eviction behavior an *additional* app-level cache wants as a backstop, not a conflict. `coil3.PlatformContext` is a typealias for `android.content.Context` on this target. */
internal actual fun defaultTileDiskCacheDirectory(platformContext: coil3.PlatformContext): String =
    File(platformContext.cacheDir, "tiles").path
