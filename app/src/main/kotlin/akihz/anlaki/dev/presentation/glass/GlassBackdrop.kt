package akihz.anlaki.dev.presentation.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import akihz.anlaki.dev.data.glass.GlassLayerSettings
import akihz.anlaki.dev.data.glass.GlassParameter
import timber.log.Timber
import kotlin.math.cos
import kotlin.math.sin

/** GPU display list of page content only. The navbar must never record itself. */
internal class GlassBackdrop(val layer: GraphicsLayer) {
    var coordinates: LayoutCoordinates? by mutableStateOf(null)
}

/** Remembers a backdrop whose GPU resources follow the composition lifetime. */
@Composable
internal fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/** Records this page into [backdrop] and draws it normally, without bitmap readbacks. */
internal fun Modifier.recordGlassBackdrop(backdrop: GlassBackdrop): Modifier =
    onGloballyPositioned { backdrop.coordinates = it }.drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * Draws refracted page content with [settings] and [themeTint], leaving foreground UI untouched.
 * [motionProgress] invalidates sampling while the selected pill moves in its graphics layer.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun GlassSurface(
    backdrop: GlassBackdrop,
    settings: GlassLayerSettings,
    themeTint: Color,
    modifier: Modifier = Modifier,
    motionProgress: Float = 0f
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shader = remember {
        try {
            RuntimeShader(NavbarGlassShader)
        } catch (error: IllegalArgumentException) {
            Timber.e(error, "Navbar glass shader could not compile")
            null
        }
    }
    val tint = if (settings.useThemeTint) themeTint
        else Color.hsv(settings[GlassParameter.Hue], settings[GlassParameter.TintSaturation],
            settings[GlassParameter.TintBrightness])
    // A failed shader still leaves a visible bar and working navigation.
    if (shader == null) {
        Box(modifier.drawWithContent { drawRect(themeTint); drawContent() })
        return
    }
    Box(
        modifier
            .onGloballyPositioned { coordinates = it }
            // Clip the finished glass, not the blur input. Blurring a capsule-shaped
            // input would pull transparent pixels into the rounded edge.
            .clip(CircleShape)
            .graphicsLayer {
                shader.setFloatUniform("dimensions", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
                shader.setFloatUniform("refraction", settings[GlassParameter.Refraction] * density)
                shader.setFloatUniform("edgeWidth", settings[GlassParameter.EdgeWidth] * density)
                shader.setFloatUniform("dispersion", settings[GlassParameter.Dispersion] * density)
                shader.setFloatUniform("tintOpacity", settings[GlassParameter.Tint])
                shader.setColorUniform("tint", tint.toArgb())
                shader.setFloatUniform("saturation", settings[GlassParameter.Saturation])
                shader.setFloatUniform("brightness", settings[GlassParameter.Brightness])
                shader.setFloatUniform("highlight", settings[GlassParameter.Highlight])
                val angle = Math.toRadians(settings[GlassParameter.LightAngle].toDouble())
                shader.setFloatUniform("lightDirection", cos(angle).toFloat(), sin(angle).toFloat())
                renderEffect = createNavbarGlassEffect(
                    shader, settings[GlassParameter.Blur] * density
                ).asComposeRenderEffect()
            }
            .drawWithContent {
                // Capture the animation value in this draw lambda so translated pills resample.
                @Suppress("UNUSED_VARIABLE") val animationFrame = motionProgress
                val source = backdrop.coordinates
                val target = coordinates
                drawRect(themeTint)
                if (source?.isAttached == true && target?.isAttached == true) {
                    val offset = target.localPositionOf(source, Offset.Zero)
                    translate(offset.x, offset.y) { drawLayer(backdrop.layer) }
                }
            }
    )
}

/**
 * Blurs the backdrop by [blurRadiusPx] before [shader] applies refraction, tint, and lighting.
 * Zero bypasses blur entirely. Clamp keeps the rectangular input opaque at its boundaries.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun createNavbarGlassEffect(shader: RuntimeShader, blurRadiusPx: Float): RenderEffect {
    val glass = RenderEffect.createRuntimeShaderEffect(shader, "backdrop")
    if (blurRadiusPx <= 0f) return glass
    val blur = RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
    return RenderEffect.createChainEffect(glass, blur)
}
