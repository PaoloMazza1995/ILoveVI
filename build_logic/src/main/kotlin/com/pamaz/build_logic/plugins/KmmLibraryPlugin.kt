package com.pamaz.build_logic.plugins

import com.pamaz.build_logic.ext.androidLibrary
import com.pamaz.build_logic.ext.appCompileSdkVersion
import com.pamaz.build_logic.ext.appMinSdkVersion
import com.pamaz.build_logic.ext.appNamespace
import com.pamaz.build_logic.ext.configurePlatformTargets
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
                apply("com.android.kotlin.multiplatform.library")
            }
            kotlin.apply {
                configurePlatformTargets(includeAndroidTarget = false)
                androidLibrary.apply {
                    namespace = target.appNamespace.plus(target.name)
                    compileSdk = target.appCompileSdkVersion
                    minSdk = target.appMinSdkVersion
                }
            }
        }
    }

}