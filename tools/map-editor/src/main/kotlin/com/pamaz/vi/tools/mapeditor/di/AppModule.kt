package com.pamaz.vi.tools.mapeditor.di

import com.pamaz.vi.tools.mapeditor.data.AuthService
import com.pamaz.vi.tools.mapeditor.data.CommitService
import com.pamaz.vi.tools.mapeditor.data.DraftStore
import com.pamaz.vi.tools.mapeditor.data.EntryRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapPublisher
import com.pamaz.vi.tools.mapeditor.data.MapTileChecker
import com.pamaz.vi.tools.mapeditor.data.PinRepository
import com.pamaz.vi.tools.mapeditor.data.RemoteTileStorage
import com.pamaz.vi.tools.mapeditor.data.fake.FakeAuthService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeCommitService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeEntryRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapPublisher
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapTileChecker
import com.pamaz.vi.tools.mapeditor.data.fake.FakePinRepository
import com.pamaz.vi.tools.mapeditor.data.supabase.MapConfigWriter
import com.pamaz.vi.tools.mapeditor.data.supabase.MapPinRepository
import com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseCommitServiceAdapter
import com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapPublisher
import com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapTileChecker
import com.pamaz.vi.tools.mapeditor.data.supabase.SupabasePinRepositoryAdapter
import com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseRemoteTileStorage
import com.pamaz.vi.tools.mapeditor.data.supabase.auth.SupabaseAuthService
import com.pamaz.vi.tools.mapeditor.draft.FileDraftStore
import com.pamaz.vi.tools.mapeditor.error.UncaughtErrorReporter
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.supabase.SupabaseClientFactory
import com.pamaz.vi.tools.mapeditor.supabase.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import org.koin.dsl.module
import java.io.File
import com.pamaz.vi.tools.mapeditor.data.supabase.CommitRepository as SupabaseCommitRepository
import com.pamaz.vi.tools.mapeditor.data.supabase.EntryRepository as SupabaseEntryRepository
import com.pamaz.vi.tools.mapeditor.data.supabase.MapConfigRepository as SupabaseMapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.supabase.SupabasePostgrestCommitRepository as RealCommitRepository

/**
 * Plain Koin, one module, same style `shared`'s `App()` uses. [MapEditorBackend.fromEnvironment]
 * picks which set of data interfaces gets bound — `FAKE` (the default, no env vars needed) or
 * `SUPABASE` (real Supabase, same `SUPABASE_URL`/`SUPABASE_ANON_KEY` vars `:tools:tilegen` and the
 * previous session's build used, plus `MAP_EDITOR_BACKEND=supabase`). `EntryRepository`/
 * `MapConfigRepository` implement the new interfaces directly; `PinRepository`/`AuthService`/
 * `CommitService`/`MapPublisher` needed a small adapter — see each adapter class's own KDoc for
 * exactly why, and this module's README's "Where the real services plug in" section.
 */
val appModule = module {
    val backend = MapEditorBackend.fromEnvironment()
    single { backend }
    when (backend) {
        MapEditorBackend.SUPABASE -> {
            val supabaseConfig = SupabaseConfig.fromEnvironment()
            single { SupabaseClientFactory.create(supabaseConfig) }
            // PLT-08: a plain Ktor client for the one thing a Supabase SDK call can't do — a
            // bare GET of a public tile URL to verify it's actually reachable.
            single { HttpClient(OkHttp) { followRedirects = true } }

            single<EntryRepository> { SupabaseEntryRepository(get()) }
            single { MapPinRepository(get()) }
            single<PinRepository> { SupabasePinRepositoryAdapter(get()) }
            single<MapConfigRepository> { SupabaseMapConfigRepository(get()) }
            single<AuthService> { SupabaseAuthService(get(), get()) }
            single<SupabaseCommitRepository> { RealCommitRepository(get()) }
            single<CommitService> { SupabaseCommitServiceAdapter(get()) }
            single<MapTileChecker> { SupabaseMapTileChecker(get()) }
            single { MapConfigWriter(get()) }
            single<RemoteTileStorage> { SupabaseRemoteTileStorage(get()) }
            single<MapPublisher> {
                SupabaseMapPublisher(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    supabaseConfig.url
                )
            }
        }

        MapEditorBackend.FAKE -> {
            single<EntryRepository> { FakeEntryRepository() }
            single<PinRepository> { FakePinRepository() }
            // Bound both ways (concrete + interface) to the *same* instance: FakeMapPublisher
            // needs the concrete class's `publish(...)` setter, not just `loadCurrent()`.
            single { FakeMapConfigRepository() }
            single<MapConfigRepository> { get<FakeMapConfigRepository>() }
            single<AuthService> { FakeAuthService() }
            single<CommitService> { FakeCommitService() }
            single<MapTileChecker> { FakeMapTileChecker() }
            single<MapPublisher> { FakeMapPublisher(get(), get()) }
        }
    }
    single<DraftStore> {
        FileDraftStore(File(System.getProperty("user.home"), ".ilovevi-map-editor-draft.json"))
    }
    // A CoroutineExceptionHandler here is the one global catch-all for every background coroutine
    // launched from EditorState (SupervisorJob means one child failing never cancels the others) —
    // without it, an uncaught exception (the map_config crash this task fixes being one example)
    // would otherwise go to the JVM's default handler and fail silently from the user's point of
    // view, instead of UncaughtErrorDialog.kt showing it.
    single {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Swing + CoroutineExceptionHandler { _, throwable ->
                UncaughtErrorReporter.report(throwable)
            },
        )
    }
    single { EditorState(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
