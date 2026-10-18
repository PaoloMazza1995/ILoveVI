package com.pamaz.vi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Minimal placeholder: the real five-tab shell (`appMain`'s `App()`) depends on `:core:design`
 * and `:core:mapview`, neither of which targets js — see `shared/README.md`'s "Why web gets a
 * placeholder" section. `webApp` isn't part of step 8's scope; this only keeps it building.
 */
@Composable
actual fun App() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ILoveVI — web build coming later")
        }
    }
}
