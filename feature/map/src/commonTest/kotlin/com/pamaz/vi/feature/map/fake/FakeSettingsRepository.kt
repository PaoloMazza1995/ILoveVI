package com.pamaz.vi.feature.map.fake

import com.pamaz.vi.core.data.repository.Preferences
import com.pamaz.vi.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A tiny in-memory stand-in for the real [SettingsRepository], for [com.pamaz.vi.feature.map.map.MapViewModel] tests. */
internal class FakeSettingsRepository : SettingsRepository {
    private val preferencesFlow = MutableStateFlow<Preferences?>(null)

    fun setPreferences(preferences: Preferences?) {
        preferencesFlow.value = preferences
    }

    override fun preferences(): Flow<Preferences?> = preferencesFlow

    override suspend fun setPreferences(preferences: Preferences) {
        preferencesFlow.value = preferences
    }
}
