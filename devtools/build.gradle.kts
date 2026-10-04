// The devtools module (WO-005, DA-84): the debug-only dev aid. Android library with Compose foundation.
// Main scope: kernel and contracts only (G-06; V-06 checks this). Never in app's main/release scope (G-04).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.jamisuni.tangram.devtools"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    // The devtools JVM tests read the 13 packaged puzzles (test scope only, as browse has).
    testImplementation(project(":content"))
    // The devtools device tests use the 13 packaged puzzles (test scope only, as browse's androidTest has).
    androidTestImplementation(project(":content"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Pinned to 3.7.0 to avoid NoSuchMethodException on API 37 (InputManager.getInstance) from transitive 3.5.0 via ui-test-junit4.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The A4 test inputs (DA-88/89): the file set S, defined once. Never build/, .gradle/ or .swdev/.
val a4FileSetS = listOf(
    "app/src/main/**",
    "play/src/main/**",
    "browse/src/main/**",
    "kernel/src/main/**",
    "contracts/src/main/**",
    "content/src/main/**",
    "store/src/main/**",
    "settings/src/main/**",
    "app/src/release/**",
    "app/build.gradle.kts",
    "settings.gradle.kts",
)

tasks.withType<Test>().configureEach {
    inputs.files(fileTree(rootDir) { include(a4FileSetS) })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // The DA-72 test also reads the debug DebugAids; a separate input, never part of S (it legitimately names devtools).
    inputs.files(fileTree(rootDir) { include("app/src/debug/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("repo.root", rootDir.absolutePath)
}
