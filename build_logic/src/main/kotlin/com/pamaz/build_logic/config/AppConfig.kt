package com.pamaz.build_logic.config

import org.gradle.api.JavaVersion

object AppConfig{
    const val APP_NAME = "app.appName"
    const val APP_ID = "app.appId"
    const val APP_NAME_NAMESPACE = "app.nameSpace"
    const val APP_COMPILE_SDK_PROPERTY = "app.compileSdkVersion"
    const val APP_TARGET_SDK_PROPERTY = "app.targetSdkVersion"
    const val APP_MIN_SDK_PROPERTY = "app.minSdkVersion"
    const val APP_VERSION_NAME_PROPERTY = "app.versionName"
    const val APP_VERSION_CODE_PROPERTY = "app.versionCode"

    val javaVersion = JavaVersion.VERSION_17

}