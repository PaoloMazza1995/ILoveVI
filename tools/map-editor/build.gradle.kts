import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.pamaz.vi.tools.mapeditor.MainKt")
}

// SupabaseConfig.fromEnvironment() / MapEditorBackend.fromEnvironment() read real process env
// vars, not Gradle properties, so these entries in the root local.properties need forwarding into
// the `run` task's child JVM explicitly.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

// IntelliJ's gutter "run" action creates its own ad-hoc JavaExec task per main function rather
// than reusing the application plugin's `run` task, so this must cover every JavaExec task, not
// just `run`, to work from the IDE too. An already-exported shell env var wins over
// local.properties — JavaExec.environment(...) would otherwise unconditionally override it.
tasks.withType<JavaExec>().configureEach {
    listOf("SUPABASE_URL", "SUPABASE_ANON_KEY", "MAP_EDITOR_BACKEND").forEach { key ->
        if (System.getenv(key) == null) {
            localProperties.getProperty(key)?.let { environment(key, it) }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:mapview"))
    implementation(project(":core:design"))
    implementation(project(":tools:tilegen"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // supabase-kt: this module's auth (real GoTrue email/password sign-in, session/JWT lifetime)
    // and multi-table PostgREST reads/upserts are enough surface that the SDK earns its keep,
    // unlike :tools:tilegen's 3 plain REST calls (see that module's README) — raw Ktor stays the
    // house style there on purpose, this is a deliberate, narrow exception.
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)

    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    implementation(libs.kotlin.csv)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    // The acceptance's "place a pin -> create a new entry -> press Commit -> success" UI test,
    // scoped to this one flow, not a general Compose UI test harness.
    testImplementation(compose.desktop.uiTestJUnit4)
}
