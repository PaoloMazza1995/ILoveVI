plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

gradlePlugin{
    plugins {
        register("kmm-app-plugin"){
            id = "kmm-app-plugin"
            implementationClass = "com.pamaz.build_logic.plugins.KmmAppPlugin"
        }
        register("kmm-library-plugin"){
            id = "kmm-library-plugin"
            implementationClass = "com.pamaz.build_logic.plugins.KmmLibraryPlugin"
        }
        register("kmm-koin-plugin"){
            id = "kmm-koin-plugin"
            implementationClass = "com.pamaz.build_logic.plugins.KmmKoinPlugin"
        }
        register("kmm-compose-plugin"){
            id = "kmm-compose-plugin"
            implementationClass = "com.pamaz.build_logic.plugins.KmmComposePlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.gradle)
    implementation(libs.com.google.devtools.ksp.gradle.plugin)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
