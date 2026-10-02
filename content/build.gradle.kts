// The content module: the puzzle library behind IPuzzleLibrary (WO-002). Pure Kotlin, no Android (ADR-002).
// Main scope may use kernel, contracts and kotlinx-serialization-json only (G-06, ADR-004).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

// Single source (G-08): Tangrams/*.json (never the schema) is copied at build time, never by hand.
val packagedDir = layout.buildDirectory.dir("generated/puzzles")
val packagePuzzles = tasks.register<Sync>("packagePuzzles") {
    from(rootProject.file("Tangrams")) {
        include("*.json")
        exclude("*.schema.json")
    }
    into(packagedDir.map { it.dir("tangrams") })
    val destination = packagedDir.get().dir("tangrams").asFile
    doLast {
        // A classpath directory cannot be listed in a jar or APK, so the index travels with the files.
        val names = destination.listFiles { f -> f.isFile && f.name.endsWith(".json") }!!
            .map { it.name }.sorted()
        File(destination, "index.txt").writeText(names.joinToString("\n", postfix = "\n"))
    }
}

sourceSets.main {
    resources.srcDir(packagedDir)
}

tasks.processResources {
    dependsOn(packagePuzzles)
}

tasks.test {
    systemProperty("tangram.root", rootDir.absolutePath)
    inputs.dir(rootProject.file("Tangrams"))
    inputs.files(rootProject.file("tools/golden/geometry.json"))
}
