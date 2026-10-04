package io.github.jamisuni.tangram.acceptance.held

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HELD-OUT (Test & Verify only): the installed app's permissions (design WO-006 section 6 item 3, DA-105). The TARGET package is read
 * through `targetContext.packageName`: the test APK's own manifest carries permissions of its own, so `context.packageName` would be the
 * wrong package. The release copy's evidence is the verifiers and the `dumpsys` line, not this test.
 */
class HeldInstalledPermissionsAppTest {
    @Suppress("DEPRECATION")
    private fun info(pkg: String) = InstrumentationRegistry.getInstrumentation().targetContext.packageManager.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)

    // REQ-010.A2 - "The installed app declares no permissions, not even network access."
    // The installed package under test requests no permission and defines none, and names nothing INTERNET. A control shows the query can
    // see permissions: the platform's own "android" package declares many.
    @Test
    fun req010_A2_theInstalledAppDeclaresNoPermissions() {
        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertFalse("targetContext is the test package $pkg", pkg.endsWith(".test"))
        val app = info(pkg)
        val requested = app.requestedPermissions?.toList().orEmpty()
        val defined = app.permissions?.map { it.name }.orEmpty()
        assertTrue("$pkg requests permissions: $requested", requested.isEmpty())
        assertTrue("$pkg defines permissions: $defined", defined.isEmpty())
        assertFalse("INTERNET is named", (requested + defined).any { it.contains("INTERNET") })

        val control = info("android")
        assertTrue("fixture: the query cannot see permissions at all", !control.permissions.isNullOrEmpty())
    }
}
