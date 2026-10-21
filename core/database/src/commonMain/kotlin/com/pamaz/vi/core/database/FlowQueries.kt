package com.pamaz.vi.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

internal fun <T : Any> Query<T>.asListFlow(): Flow<List<T>> =
    asFlow().mapToList(Dispatchers.Default)

internal fun <T : Any> Query<T>.asOneOrNullFlow(): Flow<T?> =
    asFlow().mapToOneOrNull(Dispatchers.Default)
