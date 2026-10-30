package com.pamaz.vi.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun networkHttpClientEngine(): HttpClientEngine = OkHttp.create()
