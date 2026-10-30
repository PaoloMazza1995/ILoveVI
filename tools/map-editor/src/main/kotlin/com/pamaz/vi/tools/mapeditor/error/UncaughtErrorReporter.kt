package com.pamaz.vi.tools.mapeditor.error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where an uncaught exception from a background coroutine ends up instead of failing silently
 * (Koin-free, Compose-free plain object — the `CoroutineExceptionHandler` installed on
 * `di/AppModule.kt`'s shared scope reports here; `ui/UncaughtErrorDialog.kt` observes [current]
 * and renders it).
 */
object UncaughtErrorReporter {
    private val _current = MutableStateFlow<Throwable?>(null)
    val current: StateFlow<Throwable?> = _current.asStateFlow()

    fun report(throwable: Throwable) {
        _current.value = throwable
    }

    fun dismiss() {
        _current.value = null
    }
}
