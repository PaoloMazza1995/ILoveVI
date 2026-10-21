package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.testDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferencesDaoTest {
    @Test
    fun nullUntilFirstSet() = runTest {
        val dao = PreferencesDaoImpl(testDatabase())
        assertNull(dao.preferences().first())
    }

    @Test
    fun setThenGetRoundTrips() = runTest {
        val dao = PreferencesDaoImpl(testDatabase())
        val preferences = Preferences(
            showLabels = false,
            language = "it",
            selectedNewsTopic = "NEWS",
            lastContentUpdate = null
        )

        dao.set(preferences)

        assertEquals(preferences, dao.preferences().first())
    }
}
