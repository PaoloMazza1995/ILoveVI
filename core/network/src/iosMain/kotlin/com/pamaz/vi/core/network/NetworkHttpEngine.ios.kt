package com.pamaz.vi.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun networkHttpClientEngine(): HttpClientEngine = Darwin.create()
