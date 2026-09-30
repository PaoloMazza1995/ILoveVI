package com.pamaz.build_logic.plugins

import com.pamaz.build_logic.ext.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmmComposePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target){

            plugins.run {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            kotlin.apply {
                sourceSets.apply {
                    commonMain{
                        dependencies{
                            implementation("org.jetbrains.compose.ui:ui") // Corresponds to compose.ui
                            implementation("org.jetbrains.compose.runtime:runtime") // Corresponds to compose.runtime
                            implementation("org.jetbrains.compose.foundation:foundation") // Corresponds to compose.foundation
                            implementation("org.jetbrains.compose.animation:animation")
                        }
                    }
                }
            }
        }
    }

}