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
 *
 * WO-009 (T9b), REQ-048 A3 ("The installed app still declares no permissions", REQ-010). STORE-BOUND LIMIT: this class runs on the INSTALLED
 * DEBUG build with the test APK, so it proves the app under test, not the file Play will serve. The store-bound proof is V-01 (the merged
 * release manifest), V-04, V-08 and V-09 on the release bundle's universal APK, plus the API 26 launch of a debug-key copy of that APK with
 * `dumpsys package` showing no requested permissions (TASK-099), plus the manual store-side row. The second method below is the
 * device-level statement of the criterion; the first method is REQ-010's own and is unchanged.
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

    // REQ-048.A3 - "The installed app still declares no permissions (REQ-010)."
    // Read from the installed TARGET package, with its own control: the app requests nothing (no INTERNET, no location, nothing), defines
    // nothing. The control shows
    // the query reports permissions where they exist (the platform package declares many), so an empty answer is a real answer.
    @Test
    fun req048_A3_theInstalledAppStillDeclaresNoPermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val pkg = instrumentation.targetContext.packageName
        assertFalse("targetContext is the test package $pkg", pkg.endsWith(".test"))
        assertTrue("fixture: the test package is a different package from the target", instrumentation.context.packageName != pkg)
        val app = info(pkg)
        val requested = app.requestedPermissions?.toList().orEmpty()
        val defined = app.permissions?.map { it.name }.orEmpty()
        assertTrue("$pkg requests ${requested.size} permission(s): $requested", requested.isEmpty())
        assertTrue("$pkg defines ${defined.size} permission(s): $defined", defined.isEmpty())

        val control = info("android")
        assertTrue("fixture: the query cannot see permissions at all", !control.permissions.isNullOrEmpty())
    }
}
