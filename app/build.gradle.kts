plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// compileSdk / minSdk / targetSdk and bytecode 17 come from the root build (ADR-002).
android {
    namespace = "io.github.jamisuni.tangram"

    defaultConfig {
        applicationId = "io.github.jamisuni.tangram"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    // Declared directly so the manifest's InitializationProvider stanza (G-01) always has its
    // class and lintRelease's MissingClass passes (design WO-001 §9). Version = the one already
    // resolved transitively; adds no code to the APK.
    implementation(libs.androidx.startup.runtime)

    // The composition root may depend on every module (G-06).
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(project(":content"))
    implementation(project(":play"))
    implementation(project(":store"))
    implementation(project(":browse"))
    implementation(project(":settings"))
    implementation(project(":time"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Pinned to 3.7.0 to avoid NoSuchMethodException on API 37 (InputManager.getInstance) from transitive 3.5.0 via ui-test-junit4.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(project(":devtools"))
}

// The unit-test inputs (DA-88): the files the source scan reads, declared with globs from the repo root
// so a changed file re-runs the test. Never build output, never .gradle/.
val scannedFileGlobs = listOf(
    "*/src/main/**",
    "*/src/release/**",
    "*/src/debug/**",
    "*/src/*/res/values*/strings.xml",
    "gradle/libs.versions.toml",
    "**/build.gradle.kts",
    "settings.gradle.kts",
    "Tangrams/*.json",
    "*/src/androidTest/**/PromiseWords.kt",
)

tasks.withType<Test>().configureEach {
    inputs.files(fileTree(rootDir) {
        include(scannedFileGlobs)
        // The directory names the scan skips, at any depth.
        exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/.idea/**", "**/.swdev/**", "**/Study/**", "**/Requirements/**", "**/node_modules/**")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    // The release docs (WO-009, S1): a separate named input, not part of scannedFileGlobs.
    // It may match nothing; an empty tree is a valid input.
    inputs.files(fileTree(rootDir) {
        include("release/*.md", "release/evidence/**")
    }).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("releaseDocs")
    systemProperty("repo.root", rootDir.absolutePath)
}
