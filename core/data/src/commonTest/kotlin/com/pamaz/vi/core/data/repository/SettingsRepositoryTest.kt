package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.data.testDatabase
import com.pamaz.vi.core.database.dao.Preferences
import com.pamaz.vi.core.database.dao.createPreferencesDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryTest {

    @Test
    fun preferences_nullUntilSet_thenRoundTrips() = runTest {
        val repository = SettingsRepositoryImpl(createPreferencesDao(testDatabase()))
        assertNull(repository.preferences().first())

        val preferences = Preferences(
            showLabels = false,
            language = "it",
            selectedNewsTopic = "MISSIONS",
            lastContentUpdate = null
        )
        repository.setPreferences(preferences)

        assertEquals(preferences, repository.preferences().first())
    }
}
