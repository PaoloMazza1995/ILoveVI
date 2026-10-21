package com.pamaz.vi.tools.mapeditor.importer

import com.pamaz.vi.tools.mapeditor.model.PinRow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/** Reads/writes PLT-06's JSON format: a plain array of [PinRow] (the offline store, [OfflinePinStore], reuses this exact codec). */
object PinJsonIo {
    fun read(file: File): List<PinRow> =
        json.decodeFromString(ListSerializer(PinRow.serializer()), file.readText())

    fun write(file: File, rows: List<PinRow>) {
        file.writeText(json.encodeToString(ListSerializer(PinRow.serializer()), rows))
    }
}
