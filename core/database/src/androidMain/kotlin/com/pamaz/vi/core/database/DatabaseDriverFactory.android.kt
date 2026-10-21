package com.pamaz.vi.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class PlatformContext(val context: Context)

actual class DatabaseDriverFactory actual constructor(private val context: PlatformContext) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(IloveViDatabase.Schema, context.context, "ilovevi.db")
}
