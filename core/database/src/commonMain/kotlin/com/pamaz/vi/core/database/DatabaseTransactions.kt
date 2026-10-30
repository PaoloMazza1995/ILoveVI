package com.pamaz.vi.core.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Runs [body] inside one SQL transaction, for a caller (a sync engine, e.g. `:core:data`'s
 * `ContentSyncer`) that needs several DAO writes — each DAO's own `upsert*`/`tombstone*` methods,
 * see e.g. `CatalogDao`'s KDoc ("the sync engine calls them directly") — to commit or roll back
 * together.
 *
 * SQLDelight's own [IloveViDatabase.transaction] (used directly by, e.g., `MusicDaoImpl.reorderCompilation`)
 * takes a plain, non-`suspend` lambda, because the driver underneath is synchronous; this module's
 * own DAO methods are `suspend fun`s only by API convention (every one of them is, in fact, a
 * direct, non-suspending call to a generated `*Queries` function — there is no real suspension
 * point anywhere in this database's write path). [runBlocking] here is therefore not the usual
 * "blocking inside a coroutine" anti-pattern it looks like: nothing inside [body] actually
 * suspends, so it returns immediately without ever parking the thread on real I/O — it exists
 * purely to satisfy the Kotlin compiler, which otherwise has no way to call a `suspend fun` from
 * [transaction]'s synchronous lambda type.
 */
suspend fun IloveViDatabase.inTransaction(body: suspend () -> Unit) {
    withContext(Dispatchers.Default) {
        transaction {
            runBlocking { body() }
        }
    }
}
