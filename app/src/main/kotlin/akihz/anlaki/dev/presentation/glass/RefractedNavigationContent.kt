package akihz.anlaki.dev.presentation.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import akihz.anlaki.dev.data.glass.GlassParameter
import akihz.anlaki.dev.data.glass.NavbarGlassSettings
import akihz.anlaki.dev.presentation.components.NavigationItemWidth
import akihz.anlaki.dev.presentation.components.NavigationLabelWidth
import akihz.anlaki.dev.presentation.components.NavigationItemSpacing
import akihz.anlaki.dev.presentation.components.NavigationPadding
import akihz.anlaki.dev.presentation.components.NavigationHeight
import akihz.anlaki.dev.presentation.theme.AkihzColors
import timber.log.Timber

/**
 * Refracts the white glyph mask under both glass lenses without changing layout or semantics.
 * [background] contains only the rendered glass. [labelProgress] describes the animated tab widths.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun Modifier.refractedNavigationContent(
    background: GlassBackdrop,
    settings: NavbarGlassSettings,
    selectionPosition: Float,
    labelProgress: List<Float>
): Modifier {
    val mask = rememberGraphicsLayer()
    val atlas = rememberGraphicsLayer()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageColor = AkihzColors.pageContainer
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shader = remember(labelProgress.size) {
        try {
            RuntimeShader(navigationContentShader(labelProgress.size))
        } catch (error: IllegalArgumentException) {
            Timber.e(error, "Could not compile navbar content lens")
            null
        }
    }
    if (shader == null) return this
    return onGloballyPositioned { coordinates = it }.clipToBounds().drawWithCache {
        val width = size.width
        val height = size.height
        shader.setFloatUniform("dimensions", width, height)
        val pillWidth = (NavigationItemWidth + NavigationLabelWidth).toPx()
        val padding = NavigationPadding.toPx()
        val offset = (NavigationItemWidth + NavigationItemSpacing).toPx() * selectionPosition
        shader.setFloatUniform("pillBounds", if (rtl) width - padding - pillWidth - offset else padding + offset,
            padding, pillWidth, NavigationHeight.toPx())
        shader.setFloatUniform("outerLens", settings.outer[GlassParameter.Refraction] * density,
            settings.outer[GlassParameter.EdgeWidth] * density)
        shader.setFloatUniform("pillLens", settings.pill[GlassParameter.Refraction] * density,
            settings.pill[GlassParameter.EdgeWidth] * density)
        shader.setFloatUniform("strength", settings.contentRefraction)
        shader.setFloatUniform("automaticContrast", if (settings.automaticContrast) 1f else 0f)
        shader.setFloatUniform("fixedInk", if (pageColor.luminance() > 0.179f) 0f else 1f)
        shader.setFloatUniform("outlineRadius", 0.65f * density)
        var start = padding
        val regions = FloatArray(labelProgress.size * 2)
        labelProgress.forEachIndexed { index, progress ->
            val itemWidth = (NavigationItemWidth + NavigationLabelWidth * progress).toPx()
            val center = start + itemWidth * 0.5f
            regions[index * 2] = if (rtl) width - center else center
            regions[index * 2 + 1] = itemWidth
            start += itemWidth + NavigationItemSpacing.toPx()
        }
        shader.setFloatUniform("regions", *regions)
        atlas.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "atlas").asComposeRenderEffect()
        onDrawWithContent {
            mask.record { this@onDrawWithContent.drawContent() }
            val source = background.coordinates
            val target = coordinates
            // Top half: rendered glass. Bottom half: the unblurred glyph mask.
            // Both stay on the GPU and neither captures this final composite.
            atlas.record(size = IntSize(width.toInt(), height.toInt() * 2)) {
                clipRect(0f, 0f, width, height) {
                    drawRect(pageColor)
                    if (source?.isAttached == true && target?.isAttached == true) {
                        val offset = target.localPositionOf(source, Offset.Zero)
                        translate(offset.x, offset.y) { drawLayer(background.layer) }
                    }
                }
                translate(top = height) { drawLayer(mask) }
            }
            drawLayer(atlas)
        }
    }
}
