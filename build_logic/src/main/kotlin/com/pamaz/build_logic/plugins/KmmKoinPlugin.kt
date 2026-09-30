package com.pamaz.build_logic.plugins

import com.pamaz.build_logic.ext.koinAnnotationsDependency
import com.pamaz.build_logic.ext.koinCompiler
import com.pamaz.build_logic.ext.koinComposeDependency
import com.pamaz.build_logic.ext.koinComposeViewModelDependency
import com.pamaz.build_logic.ext.koinCoreDependency
import com.pamaz.build_logic.ext.koinCoroutines
import com.pamaz.build_logic.ext.kotlin
import com.pamaz.build_logic.ext.ksp
import com.pamaz.build_logic.ext.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A special plugin used for configurating Koin, to apply in every module using koin.
 */
class KmmKoinPlugin: Plugin<Project>{

    override fun apply(target: Project) {
        with(target){

            plugins.run {
                apply("com.google.devtools.ksp")
                apply("kotlin-multiplatform")
            }

            kotlin.apply {
                sourceSets.apply{
                    commonMain{
                        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
                        dependencies {
                            api(library.koinCoreDependency())
                            api(library.koinAnnotationsDependency())
                            api(library.koinComposeViewModelDependency())
                            api(library.koinComposeDependency())
                            api(library.koinCoroutines())
                        }
                    }
                }
            }

            dependencies{
                add("kspCommonMainMetadata", library.koinCompiler())
            }

            afterEvaluate {
                tasks.filter {
                    it.name.contains("SourcesJar", true)
                }.forEach {
                    println("SourceJarTask====>${it.name}")
                    it.dependsOn("kspCommonMainKotlinMetadata")
                }
            }

            ksp.apply {
                arg("KOIN_CONFIG_CHECK", "false")
            }

        }
    }

}