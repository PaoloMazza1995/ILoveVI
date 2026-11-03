@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.pamaz.vi.core.mapview.tile

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.posix.memcpy

internal actual fun createTileDiskFiles(directoryPath: String): TileDiskFiles =
    NsFileManagerTileDiskFiles(directoryPath)

private class NsFileManagerTileDiskFiles(private val directoryPath: String) : TileDiskFiles {
    private val fileManager = NSFileManager.defaultManager

    init {
        fileManager.createDirectoryAtPath(
            directoryPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    private fun pathFor(fileName: String) = "$directoryPath/$fileName"

    override fun read(fileName: String): ByteArray? =
        NSData.dataWithContentsOfFile(pathFor(fileName))?.toByteArray()

    override fun write(fileName: String, bytes: ByteArray) {
        bytes.toNSData().writeToFile(pathFor(fileName), atomically = true)
    }

    override fun delete(fileName: String) {
        fileManager.removeItemAtPath(pathFor(fileName), error = null)
    }

    override fun list(): List<TileDiskFileInfo> {
        val names = fileManager.contentsOfDirectoryAtPath(directoryPath, error = null)
            ?.filterIsInstance<String>() ?: return emptyList()
        return names.mapNotNull { name ->
            val attributes = fileManager.attributesOfItemAtPath(pathFor(name), error = null)
                ?: return@mapNotNull null
            val size = (attributes["NSFileSize"] as? Number)?.toLong() ?: 0L
            val modifiedDate: NSDate? = attributes["NSFileModificationDate"] as? NSDate
            val modifiedMillis = if (modifiedDate != null) {
                (modifiedDate.timeIntervalSince1970 * 1000).toLong()
            } else {
                0L
            }
            TileDiskFileInfo(name, size, modifiedMillis)
        }
    }
}

/** iOS's app-private, OS-manageable-under-pressure cache directory — `coil3.PlatformContext` carries nothing useful here (no `android.content.Context` equivalent exists on this target), so it's ignored. */
internal actual fun defaultTileDiskCacheDirectory(platformContext: coil3.PlatformContext): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = (paths.firstOrNull() as? String) ?: NSTemporaryDirectoryFallback
    return "$base/tiles"
}

private const val NSTemporaryDirectoryFallback = "/tmp"

private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

private fun NSData.toByteArray(): ByteArray {
    val length = length.toInt()
    if (length == 0) return ByteArray(0)
    val result = ByteArray(length)
    memScoped {
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length.toULong())
        }
    }
    return result
}
