package com.pamaz.vi.core.database

import app.cash.sqldelight.driver.native.inMemoryDriver

internal actual fun testDatabase(): IloveViDatabase {
    val driver = inMemoryDriver(IloveViDatabase.Schema)
    return IloveViDatabase(driver)
}
