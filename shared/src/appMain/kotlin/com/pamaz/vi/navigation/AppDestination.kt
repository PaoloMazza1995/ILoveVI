package com.pamaz.vi.navigation

import com.pamaz.vi.design.components.IloveViNavDestination

/**
 * Every screen the app shell's single [androidx.navigation.compose.NavHost] can show (step 8).
 * [route] is that destination's NavHost route string.
 */
sealed interface AppDestination {
    val route: String

    /**
     * The five bottom-bar tabs, in bar order — matches [IloveViNavDestination.entries] 1:1 (one
     * entry each, same order), since [com.pamaz.vi.design.components.IloveViNavBar] owns the
     * actual bar UI/order and this just needs to agree with it for navigation purposes.
     */
    enum class Tab(override val route: String, val navDestination: IloveViNavDestination) :
        AppDestination {
        Map("map", IloveViNavDestination.Map),
        Catalog("catalog", IloveViNavDestination.Catalog),
        Music("music", IloveViNavDestination.Music),
        News("news", IloveViNavDestination.News),
        Settings("settings", IloveViNavDestination.Settings),
        ;

        companion object {
            /** The [Tab] whose [navDestination] is [destination]. */
            fun forNavDestination(destination: IloveViNavDestination): Tab =
                entries.first { it.navDestination == destination }
        }
    }

    /**
     * Opened on top of the tabs with a back control; never shown in the bottom bar.
     * [Detail]'s route carries the two fields an [com.pamaz.vi.core.model.EntryRef] needs to
     * resolve a real entry later (`category`, `entryId`) — there's no backend yet (step 9 is out
     * of scope here), but the navigation contract is already shaped like the real data.
     */
    sealed class Secondary(override val route: String) : AppDestination {
        data object MapFilters : Secondary("map/filters")
        data object Detail : Secondary("detail/{category}/{entryId}") {
            const val ARG_CATEGORY = "category"
            const val ARG_ENTRY_ID = "entryId"
            fun route(category: String, entryId: String) = "detail/$category/$entryId"
        }

        /** Only reachable from the compilation card in the Music tab — deliberately not a [Tab]. */
        data object Compilation : Secondary("music/compilation")
    }
}
