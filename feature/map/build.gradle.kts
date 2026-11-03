plugins {
    id("kmm-library-plugin")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:data"))
            implementation(project(":core:design"))
            implementation(project(":core:mapview"))

            implementation(libs.compose.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.components.resources)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }
    }
}

compose {
    resources {
        publicResClass = false
    }
}
