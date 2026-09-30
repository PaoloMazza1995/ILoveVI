package com.pamaz.build_logic.plugins

import com.pamaz.build_logic.ext.android
import com.pamaz.build_logic.ext.appCompileSdkVersion
import com.pamaz.build_logic.ext.appId
import com.pamaz.build_logic.ext.appNamespace
import com.pamaz.build_logic.ext.configureCompileOptions
import com.pamaz.build_logic.ext.configureDefaultConfig
import com.pamaz.build_logic.ext.configurePlatformTargets
import com.pamaz.build_logic.ext.configureTestOptions
import com.pamaz.build_logic.ext.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A plugin used for app's module configuration.
 */

class KmmAppPlugin: Plugin<Project> {


    override fun apply(target: Project) {
        with(target){
            plugins.run {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.multiplatform")
            }
            android.apply {
                namespace = project.appNamespace
                compileSdkVersion(project.appCompileSdkVersion)
                configureDefaultConfig(project)
                defaultConfig{
                    applicationId = target.appId
                    setProperty("archivesBaseName", "$applicationId-v$versionName($versionCode)")
                }
                configureCompileOptions()
                configureTestOptions()
                packagingOptions {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = true
                    }
                }
            }
            kotlin.apply {
                configurePlatformTargets()
            }
        }
    }

}