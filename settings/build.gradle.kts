// The settings module (WO-007, TASK-050): the settings screen code home. Android library with Compose foundation.
// Main scope: kernel and contracts only (G-06; V-06 checks this).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.jamisuni.tangram.settings"

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

    // Test scope may use content (G-06; V-06 reads main-scope configurations only).
    testImplementation(libs.junit)
    testImplementation(project(":content"))
    androidTestImplementation(project(":content"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Pinned to 3.7.0 to avoid NoSuchMethodException on API 37 (InputManager.getInstance) from transitive 3.5.0 via ui-test-junit4.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The locked REQ texts the settings unit tests read (a named input, so a change re-runs them).
tasks.withType<Test>().configureEach {
    inputs.files(fileTree(rootDir) { include("Requirements/reqs/REQ-009.md", "Requirements/reqs/REQ-049.md") })
        .withPropertyName("lockedReqTexts")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree(rootDir) { include("settings/src/main/res/values*/strings.xml") })
        .withPropertyName("settingsStrings")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("repo.root", rootDir.absolutePath)
}
