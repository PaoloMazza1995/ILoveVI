package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.PreferencesDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User preferences — local-only, nothing here is ever synced. [Preferences] is this module's own type (see [RepositoryTypes.kt]'s KDoc for why it isn't `:core:database`'s). */
interface SettingsRepository {
    /** Null until the first [setPreferences] call. */
    fun preferences(): Flow<Preferences?>
    suspend fun setPreferences(preferences: Preferences)
}

internal class SettingsRepositoryImpl(private val preferencesDao: PreferencesDao) :
    SettingsRepository {
    override fun preferences(): Flow<Preferences?> =
        preferencesDao.preferences().map { prefs ->
            prefs?.let {
                Preferences(
                    showLabels = it.showLabels,
                    language = it.language,
                    selectedNewsTopic = it.selectedNewsTopic,
                    lastContentUpdate = it.lastContentUpdate,
                )
            }
        }

    override suspend fun setPreferences(preferences: Preferences) = preferencesDao.set(
        com.pamaz.vi.core.database.dao.Preferences(
            showLabels = preferences.showLabels,
            language = preferences.language,
            selectedNewsTopic = preferences.selectedNewsTopic,
            lastContentUpdate = preferences.lastContentUpdate,
        ),
    )
}
