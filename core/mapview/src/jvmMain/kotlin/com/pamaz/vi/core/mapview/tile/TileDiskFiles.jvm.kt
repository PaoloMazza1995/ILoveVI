package com.pamaz.vi.core.mapview.tile

import java.io.File

internal actual fun createTileDiskFiles(directoryPath: String): TileDiskFiles =
    FileBackedTileDiskFiles(directoryPath)

/** Shared almost verbatim with the Android actual (both are plain `java.io.File`); kept as two small per-target files rather than a custom intermediate source set, matching this module's existing `ImageDecoding.jvm.kt`/`.ios.kt` precedent. */
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

/** Desktop has no app-sandboxed cache directory API, so this uses a dotfile under the user's home directory — persists across editor runs on purpose (re-panning/re-zooming the same map shouldn't re-download tiles just because the process restarted). */
internal actual fun defaultTileDiskCacheDirectory(platformContext: coil3.PlatformContext): String =
    System.getProperty("user.home") + "/.ilovevi/tile-cache"
