import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    android {
       namespace = "com.pamaz.vi.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        // :core:design and :core:mapview only target jvm/android/iOS (see their own
        // build.gradle.kts) — they have no js/wasmJs variant, so the real app shell (and its
        // dependency on both modules) can't live in commonMain, where webApp's js/wasmJs targets
        // would fail to resolve it. It lives on disk at src/appMain/kotlin instead, and is added
        // as an *extra source directory* of both androidMain and iosMain below — not a real
        // Kotlin source set/`dependsOn` edge, deliberately: Kotlin's default hierarchy template
        // has no built-in grouping for exactly "android + iOS, not js/wasmJs", and introducing a
        // custom intermediate source set for it disables the default template's own automatic
        // wiring project-wide (confirmed: doing so disconnected the pre-existing `iosMain`/
        // `Platform.ios.kt` from actually compiling at all). Aliasing the same directory into
        // both leaf source sets avoids that entirely, at the cost of each file being compiled
        // twice (once per platform) instead of once — see `shared/README.md`.
        // `App()` itself stays `expect` in commonMain; `src/appMain` is where its real `actual`
        // lives (web gets its own minimal placeholder `actual`, see jsMain/wasmJsMain).
        val appMainDependencies: KotlinDependencyHandler.() -> Unit = {
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(project(":core:design"))
            implementation(project(":core:mapview"))

            // Step 9's own first real consumer of the data layer: :core:database/:core:network
            // provide databaseModule/networkModule, :core:data provides dataModule — all three are
            // wired into allAppKoinModules below for the first time (previously unwired, per their
            // own READMEs, "until a consuming step needs it" — this is that step).
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(project(":core:data"))
            implementation(project(":feature:catalog"))
        }
        androidMain {
            kotlin.srcDir("src/appMain/kotlin")
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.compose.uiTooling)
                appMainDependencies()
            }
        }
        iosMain {
            kotlin.srcDir("src/appMain/kotlin")
            dependencies {
                appMainDependencies()
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Only for di/PlatformContextModule.kt's `expect fun rememberPlatformContextModule():
            // Module` signature — koin-core is fully multiplatform (js/wasmJs included), unlike
            // core:design/core:mapview, so this one addition is safe in true commonMain.
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // Mirrors the appMain/androidMain/iosMain srcDir aliasing above, for the (currently
        // Android-host-test-only — see shared/README.md) tests covering that code.
        getByName("androidHostTest") {
            kotlin.srcDir("src/appTest/kotlin")
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}