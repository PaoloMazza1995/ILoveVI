plugins {
    id("kmm-library-plugin")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:database"))
            implementation(project(":core:network"))

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.sqldelight.sqlite.driver)
            // Only `LocalSupabaseAcceptanceTest` needs a real SupabaseClient type visible, to point
            // it at a local stack instead of :core:network's build-time-configured production one.
            implementation(libs.supabase.postgrest)
        }

        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

dependencies {
    "jvmTestImplementation"(platform(libs.supabase.bom))
}
