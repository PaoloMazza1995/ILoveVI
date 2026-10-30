package com.pamaz.vi.core.data

import com.pamaz.vi.core.database.IloveViDatabase

/** A fresh, schema-migrated, in-memory [IloveViDatabase] — a new one per test, never shared. Same two-line shape `:core:database`'s own (`internal`, so unreachable from here) `testDatabase()` uses. */
internal expect fun testDatabase(): IloveViDatabase
