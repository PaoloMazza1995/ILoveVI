package com.pamaz.vi.feature.settings.fake

import com.pamaz.vi.core.data.repository.Preferences
import com.pamaz.vi.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A tiny in-memory stand-in for the real [SettingsRepository], for [com.pamaz.vi.feature.settings.settings.SettingsViewModel] tests. */
internal class FakeSettingsRepository(initial: Preferences? = null) : SettingsRepository {
    private val preferencesFlow = MutableStateFlow(initial)

    override fun preferences(): Flow<Preferences?> = preferencesFlow

    override suspend fun setPreferences(preferences: Preferences) {
        preferencesFlow.value = preferences
    }

    /** Direct, non-suspending read for test assertions — [preferences] only exposes the interface-typed plain [Flow]. */
    fun currentPreferences(): Preferences? = preferencesFlow.value
}
