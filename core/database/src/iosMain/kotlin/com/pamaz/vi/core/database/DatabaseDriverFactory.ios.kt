package com.pamaz.vi.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class PlatformContext

actual class DatabaseDriverFactory actual constructor(private val context: PlatformContext) {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(IloveViDatabase.Schema, "ilovevi.db")
}
