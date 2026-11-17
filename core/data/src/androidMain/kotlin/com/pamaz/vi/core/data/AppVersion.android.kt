package com.pamaz.vi.core.data

import com.pamaz.vi.core.database.PlatformContext

actual fun appVersionName(context: PlatformContext): String = try {
    val packageInfo = context.context.packageManager.getPackageInfo(context.context.packageName, 0)
    packageInfo.versionName ?: "?"
} catch (e: Exception) {
    "?"
}
