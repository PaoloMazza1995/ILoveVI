package com.pamaz.vi.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Opaque per-platform handle `DatabaseDriverFactory` needs to open a driver (a real
 * `android.content.Context` on Android; an unused marker everywhere else) — see this module's
 * README's "Platform drivers" section.
 */
expect class PlatformContext

/** Builds the platform [SqlDriver] backing [IloveViDatabase]. See `androidMain`/`iosMain`/`jvmMain` for the actuals. */
expect class DatabaseDriverFactory(context: PlatformContext) {
    fun createDriver(): SqlDriver
}

/** Opens [IloveViDatabase] on top of the driver [driverFactory] builds. */
fun createDatabase(driverFactory: DatabaseDriverFactory): IloveViDatabase =
    IloveViDatabase(driverFactory.createDriver())
