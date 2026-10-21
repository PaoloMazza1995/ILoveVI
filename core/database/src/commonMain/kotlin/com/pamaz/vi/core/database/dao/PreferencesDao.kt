package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asOneOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

data class Preferences(
    val showLabels: Boolean,
    val language: String,
    val selectedNewsTopic: String?,
    val lastContentUpdate: Instant?,
)

interface PreferencesDao {
    /** Null until the first [set] call (no row exists yet). */
    fun preferences(): Flow<Preferences?>
    suspend fun set(preferences: Preferences)
}

internal class PreferencesDaoImpl(database: IloveViDatabase) : PreferencesDao {
    private val preferencesQueries = database.preferencesQueries

    override fun preferences(): Flow<Preferences?> =
        preferencesQueries.select(::preferencesRowToPreferences).asOneOrNullFlow()

    override suspend fun set(preferences: Preferences) {
        preferencesQueries.upsert(
            showLabels = if (preferences.showLabels) 1L else 0L,
            language = preferences.language,
            selectedNewsTopic = preferences.selectedNewsTopic,
            lastContentUpdate = preferences.lastContentUpdate?.toEpochMilliseconds(),
        )
    }
}

private fun preferencesRowToPreferences(
    id: Long,
    showLabels: Long,
    language: String,
    selectedNewsTopic: String?,
    lastContentUpdate: Long?,
): Preferences = Preferences(
    showLabels = showLabels != 0L,
    language = language,
    selectedNewsTopic = selectedNewsTopic,
    lastContentUpdate = lastContentUpdate?.let(Instant::fromEpochMilliseconds),
)
