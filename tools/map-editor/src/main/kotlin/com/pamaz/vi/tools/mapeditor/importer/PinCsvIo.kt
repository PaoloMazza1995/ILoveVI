package com.pamaz.vi.tools.mapeditor.importer

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.pamaz.vi.tools.mapeditor.model.PinRow
import java.io.File

private val COLUMNS = listOf("category", "entry_id", "x", "y", "zone")

/** Reads/writes PLT-06's CSV format: `category, entry_id, x, y, zone` — one header row, no map_version column (that's the JSON/offline shape only). */
object PinCsvIo {
    fun read(file: File): List<PinRow> =
        csvReader().readAllWithHeader(file).map { line ->
            PinRow(
                category = line.getValue("category"),
                entryId = line.getValue("entry_id"),
                x = line.getValue("x").trim().toFloat(),
                y = line.getValue("y").trim().toFloat(),
                zone = line["zone"]?.ifBlank { null },
            )
        }

    fun write(file: File, rows: List<PinRow>) {
        csvWriter().open(file) {
            writeRow(COLUMNS)
            for (row in rows) {
                writeRow(row.category, row.entryId, row.x, row.y, row.zone ?: "")
            }
        }
    }
}
