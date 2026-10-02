// The governed interfaces and their contract types (architecture.md §4). Pure Kotlin.
// Bytecode 17 comes from the root build (ADR-002).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Contract types are built from kernel value types (PieceId, Turn, PuzzleState, ExactPoint).
    api(project(":kernel"))
}
