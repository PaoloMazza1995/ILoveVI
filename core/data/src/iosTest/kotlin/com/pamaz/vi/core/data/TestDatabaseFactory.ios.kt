package com.pamaz.vi.core.data

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.pamaz.vi.core.database.IloveViDatabase

internal actual fun testDatabase(): IloveViDatabase {
    val driver = inMemoryDriver(IloveViDatabase.Schema)
    return IloveViDatabase(driver)
}
