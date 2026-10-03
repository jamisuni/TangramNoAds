package io.github.jamisuni.tangram

import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import io.github.jamisuni.tangram.kernel.layout.LayoutRules

/** O-05 / O-09 shell: edge to edge, the F3 orientation policy, the app state in a ViewModel (DA-26, WO-004). */
class MainActivity : ComponentActivity() {
    private lateinit var model: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // decisions F3: the device-level smallest width, re-evaluated on every (re)creation. Set before
        // super.onCreate so a phone launched in landscape is not created and then recreated (CR-2 N8).
        requestedOrientation =
            if (LayoutRules.lockPortrait(Resources.getSystem().configuration.smallestScreenWidthDp)) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        model = ViewModelProvider(this, AppViewModel.Factory(filesDir))[AppViewModel::class.java]
        model.host.session?.interruptDrag() // F5: window change
        setContent { TangramApp(model.controller, model.host, model.aids) }
    }

    override fun onPause() {
        model.host.session?.interruptDrag() // F5: app hidden; first, so the save holds the settled board
        model.controller.persist() // DA-49
        super.onPause()
    }

    override fun onStop() {
        model.store.sync() // DA-47: force the last save to disk
        super.onStop()
    }
}
