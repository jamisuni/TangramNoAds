package io.github.jamisuni.tangram

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList

/**
 * Debug-only (decision DA-102): the real [MainActivity] under a per-launch locale list and font scale from [TestConfig].
 * Nothing global is touched; no system setting changes. Absent from release.
 *
 * Dependency (CR-5 N15, design section 3): the override is pinned once, at attach time, with `createConfigurationContext`.
 * It holds only because [MainActivity] declares no `configChanges`, so every configuration change recreates the activity
 * and runs `attachBaseContext` again. Adding `configChanges` to [MainActivity] would let a later configuration change
 * bypass the pin; do not add it without revisiting this class.
 */
class LocaleOverrideActivity : MainActivity() {
    override fun attachBaseContext(newBase: Context) {
        val tags = TestConfig.languageTags
        val scale = TestConfig.fontScale
        if (tags == null && scale == null) {
            super.attachBaseContext(newBase)
            return
        }
        val config = Configuration(newBase.resources.configuration)
        tags?.let { config.setLocales(LocaleList.forLanguageTags(it)) }
        scale?.let { config.fontScale = it }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
