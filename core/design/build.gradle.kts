plugins {
    id("kmm-library-plugin")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.animation)
            implementation(libs.compose.ui.util)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)

            implementation(libs.navigation.compose)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.base)
            implementation(libs.coil.ktor)
            implementation(libs.bundles.compottie)
            implementation(libs.jetbrains.material.icons)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    configurations.all {
        resolutionStrategy {
            force("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
            exclude(group = "io.coil-kt.coil3", module = "coil-network-ktor2")
        }
    }
}

compose{
    resources{
        publicResClass = true
    }
}