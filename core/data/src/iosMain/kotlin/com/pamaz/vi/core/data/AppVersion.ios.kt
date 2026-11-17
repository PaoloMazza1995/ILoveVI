package com.pamaz.vi.core.data

import com.pamaz.vi.core.database.PlatformContext
import platform.Foundation.NSBundle

actual fun appVersionName(context: PlatformContext): String =
    (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "?"
