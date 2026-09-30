package com.pamaz.build_logic.plugins

import com.pamaz.build_logic.ext.android
import com.pamaz.build_logic.ext.appCompileSdkVersion
import com.pamaz.build_logic.ext.appNamespace
import com.pamaz.build_logic.ext.configureCompileOptions
import com.pamaz.build_logic.ext.configureDefaultConfig
import com.pamaz.build_logic.ext.configurePlatformTargets
import com.pamaz.build_logic.ext.configureTestOptions
import com.pamaz.build_logic.ext.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A plugin used for every apps library module configuration.
 */
class KmmLibraryPlugin : Plugin<Project>{

    override fun apply(target: Project) {
        with(target){
            plugins.apply {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.library")
            }
            android.apply {
                compileSdkVersion(target.appCompileSdkVersion)
                namespace = target.appNamespace.plus(target.name)
                configureDefaultConfig(target)
                configureCompileOptions()
                configureTestOptions()
            }
            kotlin.apply {
                configurePlatformTargets()
            }
        }
    }

}