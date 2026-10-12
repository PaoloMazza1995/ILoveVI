package com.pamaz.vi.tools.tilegen

import java.nio.file.Path

/**
 * Where the full-resolution source map image comes from. A sealed type (rather than a raw
 * `String`) so the pipeline can never confuse "download this HTTPS URL" with "read this local
 * file" — the two paths have very different validation (HTTPS-only, size limit, content-type
 * check) and failure modes.
 */
sealed interface TileSource {
    /** Downloaded once into a temp folder before tiling; see [com.pamaz.vi.tools.tilegen.SourceImageFetcher]. */
    data class Remote(val url: String) : TileSource

    /** Read directly from disk; never copied or uploaded as-is, only tiled. */
    data class Local(val path: Path) : TileSource
}
