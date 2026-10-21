package com.pamaz.vi.tools.mapeditor.importer

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.model.PinRow
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.model.toEntryRefOrNull

/** One row that passed validation, ready to become (or update) a [com.pamaz.vi.core.model.MapPin]. */
data class ValidatedPinRow(val entryRef: EntryRef, val position: NormalizedPoint, val zone: String?)

/** One row that failed, with a 1-based [rowNumber] (matching what a spreadsheet/CSV editor shows) and a human-readable [reason]. */
data class ImportIssue(val rowNumber: Int, val row: PinRow, val reason: String)

/**
 * The result of validating a CSV/JSON import (PLT-06): every row that passed, every row that
 * didn't and why, shown to the admin as a report they must confirm before anything is written —
 * this type never writes anything itself.
 */
data class ImportReport(
    val totalRows: Int,
    val validRows: List<ValidatedPinRow>,
    val issues: List<ImportIssue>
) {
    val isClean: Boolean get() = issues.isEmpty()
}

/**
 * Validates imported [PinRow]s against the three rules PLT-06 calls out — unknown entry,
 * out-of-range x/y, duplicates — in that order per row, so a row only ever gets *one* issue (the
 * first one that applies) rather than being reported multiple times.
 */
object ImportValidator {
    /** [knownEntries] is every (category, raw entry id) pair the backend actually has — usually all non-deleted weapons/vehicles/items. */
    fun validate(rows: List<PinRow>, knownEntries: Set<Pair<String, String>>): ImportReport {
        val issues = mutableListOf<ImportIssue>()
        val validRows = mutableListOf<ValidatedPinRow>()
        val seenKeys = mutableSetOf<Pair<String, String>>()

        rows.forEachIndexed { index, row ->
            val rowNumber = index + 1
            val entryRef = row.toEntryRefOrNull()
            val key = row.category.trim().uppercase() to row.entryId

            when {
                entryRef == null -> issues += ImportIssue(
                    rowNumber,
                    row,
                    "unknown category \"${row.category}\" (expected WEAPON, VEHICLE or ITEM)",
                )

                key !in knownEntries -> issues += ImportIssue(
                    rowNumber, row, "unknown entry: no ${row.category} with id \"${row.entryId}\"",
                )

                row.x !in 0f..1f || row.y !in 0f..1f -> issues += ImportIssue(
                    rowNumber, row, "x/y out of range 0..1 (x=${row.x}, y=${row.y})",
                )

                !seenKeys.add(key) -> issues += ImportIssue(
                    rowNumber,
                    row,
                    "duplicate row for the same entry (${row.category} ${row.entryId})",
                )

                else -> validRows += ValidatedPinRow(
                    entryRef,
                    NormalizedPoint(row.x, row.y),
                    row.zone?.ifBlank { null })
            }
        }
        return ImportReport(rows.size, validRows, issues)
    }
}

/** The inverse of validation: what an export/offline-save row looks like for one already-valid pin. */
fun ValidatedPinRow.toPinRow(mapVersion: Int? = null): PinRow = PinRow(
    category = entryRef.category.name,
    entryId = entryRef.rawEntryId(),
    x = position.x,
    y = position.y,
    zone = zone,
    mapVersion = mapVersion,
)
