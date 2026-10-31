package com.pamaz.vi.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.core.module.Module
import org.koin.dsl.module

/** Never actually called: `App()`'s wasmJs `actual` is a placeholder that doesn't start Koin at all (see `shared/README.md`). Exists only to satisfy the `expect`/`actual` contract for this target. */
@Composable
actual fun rememberPlatformContextModule(): Module = remember { module { } }
