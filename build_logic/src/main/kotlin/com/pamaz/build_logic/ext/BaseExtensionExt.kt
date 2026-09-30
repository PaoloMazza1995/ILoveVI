package com.pamaz.build_logic.ext

import com.android.build.gradle.BaseExtension
import com.pamaz.build_logic.config.AppConfig
import org.gradle.api.Project

internal fun BaseExtension.configureCompileOptions() {
    compileOptions {
        sourceCompatibility = AppConfig.javaVersion
        targetCompatibility = AppConfig.javaVersion
    }
}

internal fun BaseExtension.configureTestOptions() {
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

internal fun BaseExtension.configureDefaultConfig(project: Project) {
    project.android.apply {
        namespace = project.appNamespace
        compileSdkVersion(project.appCompileSdkVersion)
    }
    defaultConfig {
        minSdk = project.appMinSdkVersion
        targetSdk = project.appTargetSdkVersion
        versionCode = project.appVersionCode
        versionName = project.appVersionName
    }
}
