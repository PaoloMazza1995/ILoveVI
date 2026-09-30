package com.pamaz.build_logic.ext

import com.android.build.gradle.BaseExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.android: BaseExtension
    get() = extensions.findByName("android") as? BaseExtension
        ?: error("This is not an Android module!")

internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
        ?: error("This is not a Kotlin Multiplatform module!")

internal val Project.library: VersionCatalog
    get() {
        return project.extensions.getByType<VersionCatalogsExtension>()
            .named("libs")
    }

internal val Project.ksp: KspExtension
    get() {
        return project.extensions.findByName("ksp") as? KspExtension
            ?:error("This is not a Ksp module!")
    }





