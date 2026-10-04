package io.github.jamisuni.tangram.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The gear in the top bar (REQ-032): a 48 x 48 dp touch area (REQ-037, DA-53). The glyph is drawn in px with
 * `drawLine` and `drawCircle`, never a path and never under a canvas scale (DA-92: a scaled path blurs on API 26).
 */
@Composable
fun SettingsGear(controller: SettingsController, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.settings_open_description)
    Canvas(
        modifier = modifier
            .testTag("settings-button")
            .size(48.dp)
            .clickable(role = Role.Button, onClick = controller::open)
            .semantics { contentDescription = description },
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val u = size.minDimension / 24f // one design unit in px: a plain multiplication, no canvas scale
        for (i in 0 until 8) {
            val a = Math.PI / 4.0 * i
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                color = SettingsStyle.MUTED,
                start = Offset(c.x + dx * 7f * u, c.y + dy * 7f * u),
                end = Offset(c.x + dx * 10f * u, c.y + dy * 10f * u),
                strokeWidth = 3f * u,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = SettingsStyle.MUTED, radius = 5f * u, center = c, style = Stroke(width = 2.8f * u))
    }
}
