package com.pamaz.vi.core.data

/** "Android"/"iOS"/"JVM" — used only to decide which Google/Apple sign-in flow [AccountServiceImpl] takes (`docs/auth-notes.md` §1/§3), nothing user-visible. */
expect fun getPlatformName(): String
