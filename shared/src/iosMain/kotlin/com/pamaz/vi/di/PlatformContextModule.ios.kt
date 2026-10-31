package com.pamaz.vi.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.pamaz.vi.core.database.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

@Composable
actual fun rememberPlatformContextModule(): Module = remember {
    module { single { PlatformContext() } }
}
