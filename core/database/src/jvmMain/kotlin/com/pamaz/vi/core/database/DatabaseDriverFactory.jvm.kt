package com.pamaz.vi.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class PlatformContext

actual class DatabaseDriverFactory actual constructor(private val context: PlatformContext) {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(System.getProperty("user.home"), ".ilovevi/ilovevi.db")
        dbFile.parentFile?.mkdirs()
        val isNewDatabase = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNewDatabase) {
            IloveViDatabase.Schema.create(driver)
        }
        return driver
    }
}
