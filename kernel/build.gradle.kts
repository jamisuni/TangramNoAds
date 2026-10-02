// The kernel: TYPE value rules, implemented once (D1). Pure Kotlin, no Android (ADR-002).
// Bytecode 17 comes from the root build, so the Android modules can dex this module.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(libs.junit)
    // Test scope only: the kernel tests read tools/golden/geometry.json (G-03, design WO-001 §8).
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    // Where the tests find Tangrams/ and tools/golden/geometry.json.
    systemProperty("tangram.root", rootDir.absolutePath)
    // The tests read these files, so they are task inputs: without them org.gradle.caching=true
    // could replay a cached pass after a puzzle or the golden changed (design WO-001 §10).
    inputs.dir(rootProject.file("Tangrams"))
    // `files`, not `file`: the golden does not exist until TASK-003a writes it, and `files`
    // tolerates a missing path (treated as absent; appearing later changes the fingerprint).
    inputs.files(rootProject.file("tools/golden/geometry.json"))
}
