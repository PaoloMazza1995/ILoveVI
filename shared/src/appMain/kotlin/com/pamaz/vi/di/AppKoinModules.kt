package com.pamaz.vi.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * One (currently empty) Koin module per Gradle module that might need DI later — step 8 asks for
 * "one module per Gradle module aggregated in the app module (an empty aggregate is fine now)".
 * Nothing in this step reads from a database, network or backend (step 9's job), so there is
 * nothing to actually provide yet; each `module {}` is a placeholder a later step fills in without
 * the app module's aggregation code needing to change.
 */
internal val coreModelModule: Module = module { }
internal val coreDesignModule: Module = module { }
internal val coreMapviewModule: Module = module { }

/** The app module's own screens/navigation have no DI needs yet either. */
internal val appModule: Module = module { }

/** Every module's Koin module, aggregated in the one place (`App()`, in `com.pamaz.vi`) that starts Koin. */
internal val allAppKoinModules: List<Module> =
    listOf(coreModelModule, coreDesignModule, coreMapviewModule, appModule)
