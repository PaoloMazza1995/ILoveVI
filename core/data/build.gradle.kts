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

            // AccountServiceImpl (step 13B): signInWith(IDToken/Google), the delete_own_account
            // RPC — this module's own `implementation`, not leaked from :core:network's (that
            // module's own `implementation(libs.supabase.auth)` isn't visible here either, same
            // "implementation, never api" convention `core:design`'s README already documents).
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            // Google sign-in: Credential Manager + ID token (docs/auth-notes.md §1).
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.sqldelight.sqlite.driver)
        }

        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

dependencies {
    "commonMainImplementation"(platform(libs.supabase.bom))
}
