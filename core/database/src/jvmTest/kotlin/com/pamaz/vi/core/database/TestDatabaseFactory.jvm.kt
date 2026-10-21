package com.pamaz.vi.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

internal actual fun testDatabase(): IloveViDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    IloveViDatabase.Schema.create(driver)
    return IloveViDatabase(driver)
}
