package com.pamaz.vi.tools.mapeditor.importer

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.tools.mapeditor.model.PinRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportValidatorTest {

    private val known = setOf("WEAPON" to "w1", "VEHICLE" to "v1", "ITEM" to "i1")

    @Test
    fun validate_acceptsEveryWellFormedRow() {
        val rows = listOf(
            PinRow("WEAPON", "w1", 0.1f, 0.2f, "Zone A"),
            PinRow("VEHICLE", "v1", 0.5f, 0.5f, null),
            PinRow("ITEM", "i1", 1f, 0f, ""),
        )
        val report = ImportValidator.validate(rows, known)
        assertTrue(report.isClean)
        assertEquals(3, report.validRows.size)
        assertEquals(0, report.issues.size)
        assertIs<EntryRef.ToWeapon>(report.validRows[0].entryRef)
    }

    @Test
    fun validate_rejectsUnknownCategory() {
        val report = ImportValidator.validate(listOf(PinRow("SPELL", "w1", 0.1f, 0.1f)), known)
        assertEquals(1, report.issues.size)
        assertTrue(report.issues[0].reason.contains("unknown category"))
    }

    @Test
    fun validate_rejectsUnknownEntry() {
        val report =
            ImportValidator.validate(listOf(PinRow("WEAPON", "does-not-exist", 0.1f, 0.1f)), known)
        assertEquals(1, report.issues.size)
        assertTrue(report.issues[0].reason.contains("unknown entry"))
    }

    @Test
    fun validate_rejectsOutOfRangeCoordinates() {
        val report = ImportValidator.validate(
            listOf(PinRow("WEAPON", "w1", 1.5f, 0.1f), PinRow("WEAPON", "w1", 0.1f, -0.1f)),
            known,
        )
        // Both rows target the same entry, but the *first* issue found (range) wins over duplicate.
        assertEquals(2, report.issues.size)
        assertTrue(report.issues.all { it.reason.contains("out of range") })
    }

    @Test
    fun validate_rejectsDuplicateRowsForTheSameEntry() {
        val rows = listOf(PinRow("WEAPON", "w1", 0.1f, 0.1f), PinRow("WEAPON", "w1", 0.2f, 0.2f))
        val report = ImportValidator.validate(rows, known)
        assertEquals(1, report.validRows.size)
        assertEquals(1, report.issues.size)
        assertTrue(report.issues[0].reason.contains("duplicate"))
    }

    @Test
    fun validate_handles300Rows() {
        val rows = (1..300).map { PinRow("ITEM", "i1-$it", 0.5f, 0.5f) }
        val knownMany = rows.map { "ITEM" to it.entryId }.toSet()
        val report = ImportValidator.validate(rows, knownMany)
        assertEquals(300, report.totalRows)
        assertTrue(report.isClean)
        assertEquals(300, report.validRows.size)
    }
}
