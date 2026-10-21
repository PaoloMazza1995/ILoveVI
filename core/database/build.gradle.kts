plugins {
    id("kmm-library-plugin")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("IloveViDatabase") {
            packageName.set("com.pamaz.vi.core.database")
            verifyMigrations.set(true)
            dialect(libs.sqldelight.dialect)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
