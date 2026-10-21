package com.pamaz.vi.tools.mapeditor.di

import com.pamaz.vi.tools.mapeditor.auth.AdminAuthRepository
import com.pamaz.vi.tools.mapeditor.data.EntryRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapPinRepository
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.supabase.SupabaseClientFactory
import com.pamaz.vi.tools.mapeditor.supabase.SupabaseConfig
import org.koin.dsl.module

/** Plain Koin, one module, same style `shared`'s `App()` uses (see that module's README) — not the dormant koin-annotations/KSP convention plugin. */
val appModule = module {
    single { SupabaseConfig.fromEnvironment() }
    single { SupabaseClientFactory.create(get()) }
    single { AdminAuthRepository(get()) }
    single { EntryRepository(get()) }
    single { MapPinRepository(get()) }
    single { MapConfigRepository(get()) }
    single { EditorState(get(), get(), get(), get()) }
}
