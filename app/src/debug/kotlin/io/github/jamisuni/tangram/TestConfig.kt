package io.github.jamisuni.tangram

import io.github.jamisuni.tangram.time.TimeSource

/**
 * Debug-only test seam (decision DA-102). Read by [LocaleOverrideActivity] at attach time; tests set it before launch
 * and reset it to null afterwards. `languageTags` is a BCP-47 list ("fi-FI", "sv-SE,fi-FI"); null means no override.
 */
object TestConfig {
    @Volatile var languageTags: String? = null
    @Volatile var fontScale: Float? = null

    /** WO-008 (DA-137): a manual clock for the play-time keeper; null means the real one. Tests reset it after use. */
    @Volatile var timeSource: TimeSource? = null
}
