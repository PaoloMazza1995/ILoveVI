package com.pamaz.vi.core.mapview.tile

/** One entry in [TileDiskFiles]' backing directory — enough to drive LRU-by-size eviction. */
internal data class TileDiskFileInfo(
    val fileName: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
)

/**
 * The minimal per-platform file operations [TileDiskCache] needs, kept as a plain interface
 * (rather than an `expect class`) so a commonTest fake can implement it directly — [TileDiskCache]'s
 * own LRU/size-budget logic is then testable on every host, no Android/iOS target required.
 * [createTileDiskFiles] is the real, per-platform factory every production caller uses.
 */
internal interface TileDiskFiles {
    fun read(fileName: String): ByteArray?
    fun write(fileName: String, bytes: ByteArray)
    fun delete(fileName: String)
    fun list(): List<TileDiskFileInfo>
}

/** Backed by a plain directory at [directoryPath] (created if missing) on every platform. */
internal expect fun createTileDiskFiles(directoryPath: String): TileDiskFiles

/**
 * Resolves a writable, app-private directory to store tile bytes in, from Coil3's own
 * [coil3.PlatformContext] — already a dependency of this module and already obtained the same way
 * by callers via `LocalPlatformContext.current` — so no second, module-specific "give me a
 * context" mechanism is needed. Android's actual needs the real context (its cache directory is
 * namespaced per app); JVM/iOS ignore it entirely, since neither platform's cache directory
 * resolution depends on an app context object.
 */
internal expect fun defaultTileDiskCacheDirectory(platformContext: coil3.PlatformContext): String

/**
 * Deterministic, low-collision (64-bit FNV-1a, not cryptographic — a cache doesn't need
 * collision-resistance against an adversary, just against accidentally serving one tile's bytes
 * for a different tile's URL) file name for [url] — tile URLs contain `/` and query-ish characters
 * that aren't safe/portable file names on every platform, and a bare [String.hashCode] (32-bit,
 * and explicitly *not* guaranteed stable across Kotlin versions) is both too collision-prone and
 * too fragile for a cache key meant to persist across app runs.
 */
internal fun tileCacheFileName(url: String): String {
    var hash = -0x61c8864680b583ebL // FNV offset basis, 0xcbf29ce484222325 as a signed Long.
    val prime = 0x100000001b3L
    for (c in url) {
        hash = hash xor c.code.toLong()
        hash *= prime
    }
    return hash.toULong().toString(36)
}
