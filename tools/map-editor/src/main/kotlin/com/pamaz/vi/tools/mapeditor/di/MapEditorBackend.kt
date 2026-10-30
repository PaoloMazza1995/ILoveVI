package com.pamaz.vi.tools.mapeditor.di

/**
 * The mock/real data switch. Read once at startup (`MAP_EDITOR_BACKEND` env var, `fake` by
 * default) rather than at runtime — this tool never flips backends mid-session, so there's no
 * reason to pay for a runtime toggle (and the two backends don't share a session/draft anyway:
 * switching would mean re-signing-in and re-pointing at a different map).
 */
enum class MapEditorBackend {
    /** The default: [com.pamaz.vi.tools.mapeditor.data.fake] implementations, no network, no env vars needed. */
    FAKE,

    /** [com.pamaz.vi.tools.mapeditor.data.supabase]'s real implementations, behind [com.pamaz.vi.tools.mapeditor.supabase.SupabaseConfig.fromEnvironment]. */
    SUPABASE,
    ;

    companion object {
        fun fromEnvironment(env: (String) -> String? = System::getenv): MapEditorBackend =
            when (env("MAP_EDITOR_BACKEND")?.trim()?.lowercase()) {
                "supabase", "real" -> SUPABASE
                else -> FAKE
            }
    }
}
