package com.pamaz.vi.core.database

/** A fresh, schema-migrated, in-memory [IloveViDatabase] — a new one per call, never shared between tests. */
internal expect fun testDatabase(): IloveViDatabase
