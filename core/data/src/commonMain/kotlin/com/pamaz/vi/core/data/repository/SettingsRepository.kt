package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.Preferences
import com.pamaz.vi.core.database.dao.PreferencesDao
import kotlinx.coroutines.flow.Flow

/** User preferences — local-only, nothing here is ever synced. [Preferences] is reused as-is from `:core:database` (same rationale as `MusicRepository`'s `Compilation` reuse). */
interface SettingsRepository {
    /** Null until the first [setPreferences] call. */
    fun preferences(): Flow<Preferences?>
    suspend fun setPreferences(preferences: Preferences)
}

internal class SettingsRepositoryImpl(private val preferencesDao: PreferencesDao) :
    SettingsRepository {
    override fun preferences(): Flow<Preferences?> = preferencesDao.preferences()
    override suspend fun setPreferences(preferences: Preferences) = preferencesDao.set(preferences)
}
