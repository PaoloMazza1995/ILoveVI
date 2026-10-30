package com.pamaz.vi.core.network

import io.ktor.client.engine.HttpClientEngine

/**
 * One real HTTP engine per platform, following this repo's `expect`/`actual` factory convention
 * (`:core:database`'s `DatabaseDriverFactory`, `:core:mapview`'s `decodeImageBitmapDownscaled`):
 * OkHttp on Android/JVM, Darwin on iOS. [SupabaseClientProvider] is the only caller.
 */
internal expect fun networkHttpClientEngine(): HttpClientEngine
