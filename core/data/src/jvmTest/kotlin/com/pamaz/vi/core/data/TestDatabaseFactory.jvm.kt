package com.pamaz.vi.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pamaz.vi.core.database.IloveViDatabase

internal actual fun testDatabase(): IloveViDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    IloveViDatabase.Schema.create(driver)
    return IloveViDatabase(driver)
}
