package com.pamaz.build_logic.ext

import com.pamaz.build_logic.config.AppConfig
import org.gradle.api.Project

val Project.appId
    get() = this.findProperty(AppConfig.APP_ID) as String

val Project.appNamespace
    get() = this.findProperty(AppConfig.APP_NAME_NAMESPACE) as String

val Project.appCompileSdkVersion
    get() = this.findProperty(AppConfig.APP_COMPILE_SDK_PROPERTY).toString().toInt()

val Project.appTargetSdkVersion
    get() = this.findProperty(AppConfig.APP_TARGET_SDK_PROPERTY).toString().toInt()

val Project.appMinSdkVersion
    get() = this.findProperty(AppConfig.APP_MIN_SDK_PROPERTY).toString().toInt()

val Project.appVersionName
    get() = this.findProperty(AppConfig.APP_VERSION_NAME_PROPERTY) as String

val Project.appVersionCode
    get() = this.findProperty(AppConfig.APP_VERSION_CODE_PROPERTY).toString().toInt()