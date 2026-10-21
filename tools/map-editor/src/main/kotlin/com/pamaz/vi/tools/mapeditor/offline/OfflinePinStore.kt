package com.pamaz.vi.tools.mapeditor.offline

import com.pamaz.vi.tools.mapeditor.importer.PinJsonIo
import com.pamaz.vi.tools.mapeditor.model.PinRow
import java.io.File

/**
 * PLT-09's "Open image" offline mode: pins placed with no backend/network are kept in one local
 * JSON file, same columns as the export format plus `map_version` (there's no `map_config` row to
 * supply it implicitly when working from a local image) — reuses [PinJsonIo]'s exact codec, this
 * is not a second JSON format.
 */
class OfflinePinStore(private val file: File) {
    fun load(): List<PinRow> = if (file.exists()) PinJsonIo.read(file) else emptyList()

    fun save(rows: List<PinRow>) {
        file.parentFile?.mkdirs()
        PinJsonIo.write(file, rows)
    }
}
