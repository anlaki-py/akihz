package akihz.anlaki.dev.presentation.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.unit.Dp
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
    labelProgress: List<Float>,
    wideItemWidth: Dp? = null,
    itemHeight: Dp = NavigationHeight
): Modifier {
    val mask = rememberGraphicsLayer()
    val atlas = rememberGraphicsLayer()
    val sample = rememberGraphicsLayer()
    val sampleRegions = remember(labelProgress.size) { FloatArray(labelProgress.size * 2) }
    val initialInk = if (AkihzColors.pageContainer.luminance() > 0.179f) 0f else 1f
    var targets by remember(labelProgress.size) { mutableStateOf(List(labelProgress.size) { initialInk }) }
    val inks = targets.map { target ->
        animateFloatAsState(target, tween(250), label = "Navbar ink")
    }
    LaunchedEffect(sample, settings.automaticContrast, labelProgress.size) {
        if (!settings.automaticContrast) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            if (sample.size.width > 0) {
                // Graphics layers return hardware bitmaps; pixel reads need a software copy.
                val bitmap = sample.toImageBitmap().asAndroidBitmap()
                    .copy(Bitmap.Config.ARGB_8888, false)
                val pixels = bitmap.asImageBitmap().toPixelMap()
                targets = List(labelProgress.size) { index ->
                    var luminance = 0f
                    for (y in -1..1) {
                        for (x in -1..1) {
                            val sx = ((sampleRegions[index * 2] +
                                x * sampleRegions[index * 2 + 1] * 0.3f) * pixels.width)
                                .toInt().coerceIn(0, pixels.width - 1)
                            val sy = ((0.5f + y * 0.2f) * pixels.height)
                                .toInt().coerceIn(0, pixels.height - 1)
                            luminance += pixels[sx, sy].luminance()
                        }
                    }
                    if (luminance / 9f > 0.179f) 0f else 1f
                }
                bitmap.recycle()
            }
            delay(50)
        }
    }
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
        val pillWidth = (wideItemWidth ?: (NavigationItemWidth + NavigationLabelWidth)).toPx()
        val padding = NavigationPadding.toPx()
        val offset = (wideItemWidth ?: (NavigationItemWidth + NavigationItemSpacing)).toPx() * selectionPosition
        shader.setFloatUniform("pillBounds", if (rtl) width - padding - pillWidth - offset else padding + offset,
            padding, pillWidth, itemHeight.toPx())
        shader.setFloatUniform("outerLens", settings.outer[GlassParameter.Refraction] * density,
            settings.outer[GlassParameter.EdgeWidth] * density)
        shader.setFloatUniform("pillLens", settings.pill[GlassParameter.Refraction] * density,
            settings.pill[GlassParameter.EdgeWidth] * density)
        shader.setFloatUniform("strength", settings.contentRefraction)
        shader.setFloatUniform("automaticContrast", if (settings.automaticContrast) 1f else 0f)
        shader.setFloatUniform("fixedInk", if (pageColor.luminance() > 0.179f) 0f else 1f)
        var start = padding
        val regions = FloatArray(labelProgress.size * 2)
        labelProgress.forEachIndexed { index, progress ->
            val itemWidth = (wideItemWidth ?: (NavigationItemWidth + NavigationLabelWidth * progress)).toPx()
            val center = start + itemWidth * 0.5f
            regions[index * 2] = if (rtl) width - center else center
            regions[index * 2 + 1] = itemWidth
            sampleRegions[index * 2] = regions[index * 2] / width
            sampleRegions[index * 2 + 1] = itemWidth / width
            start += itemWidth + if (wideItemWidth == null) NavigationItemSpacing.toPx() else 0f
        }
        shader.setFloatUniform("regions", regions)
        onDrawWithContent {
            shader.setFloatUniform("inks", inks.map { it.value }.toFloatArray())
            // RenderEffect snapshots uniforms. Recreate it after each animated ink update.
            atlas.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "atlas").asComposeRenderEffect()
            mask.record { this@onDrawWithContent.drawContent() }
            val source = background.coordinates
            val target = coordinates
            // Read back only a small glass image. Animated ink stays independent of
            // backdrop changes, including abrupt page and theme switches.
            if (settings.automaticContrast) {
                sample.record(size = IntSize(96, 16)) {
                    scale(96f / width, 16f / height, pivot = Offset.Zero) {
                        drawRect(pageColor, size = androidx.compose.ui.geometry.Size(width, height))
                        if (source?.isAttached == true && target?.isAttached == true) {
                            val offset = target.localPositionOf(source, Offset.Zero)
                            translate(offset.x, offset.y) { drawLayer(background.layer) }
                        }
                    }
                }
            }
            // Top half: rendered glass. Bottom half: the unblurred glyph mask.
            // Neither layer captures this final composite.
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
