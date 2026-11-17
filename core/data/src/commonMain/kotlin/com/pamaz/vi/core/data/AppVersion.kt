package com.pamaz.vi.core.data

import com.pamaz.vi.core.database.PlatformContext

/** Real package/bundle version — never a hand-maintained duplicate of `androidApp`'s `versionName`/iOS's `CFBundleShortVersionString`, so it can't drift. Shown by `:feature:settings`'s About section. */
expect fun appVersionName(context: PlatformContext): String
