package akihz.anlaki.dev.presentation.modifiers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val progressiveBlurShader = """
    uniform shader content;
    uniform float blurRadius;
    uniform float topHeight;
    uniform float bottomHeight;
    uniform float contentHeight;

    half4 main(float2 fragCoord) {
        float topProgress = topHeight > 0.0
            ? 1.0 - clamp(fragCoord.y / topHeight, 0.0, 1.0) : 0.0;
        float bottomProgress = bottomHeight > 0.0
            ? 1.0 - clamp((contentHeight - fragCoord.y) / bottomHeight, 0.0, 1.0) : 0.0;
        float progress = max(topProgress, bottomProgress);
        progress = pow(progress, 1.5);
        float radius = progress * blurRadius;
        if (radius <= 0.0) return content.eval(fragCoord);

        half4 accum = half4(0.0);
        float weightSum = 0.0;
        float dither = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
        float2 jitter = float2(dither - 0.5, fract(dither * 1.618) - 0.5);
        const int samples = 2;
        float offsetScale = radius / float(samples);

        for (int x = -samples; x <= samples; x++) {
            for (int y = -samples; y <= samples; y++) {
                float2 offset = (float2(float(x), float(y)) + jitter) * offsetScale;
                float distSquared = dot(offset, offset);
                float radiusSquared = radius * radius;
                if (distSquared <= radiusSquared) {
                    float weight = exp(-3.0 * distSquared / radiusSquared);
                    accum += content.eval(fragCoord + offset) * weight;
                    weightSum += weight;
                }
            }
        }
        return accum / weightSum;
    }
""".trimIndent()

/**
 * Applies one combined progressive blur layer to the top and bottom edges.
 */
internal fun Modifier.progressiveBlur(
    blurRadius: Float,
    topHeight: Float,
    bottomHeight: Float
): Modifier = composed {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var powerSaveMode by remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }
    val problematicSamsung = remember {
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        blurRadius > 0f && !problematicSamsung

    DisposableEffect(context, powerManager, supportsBlur) {
        if (!supportsBlur || powerManager == null) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context?, intent: Intent?) {
                    powerSaveMode = powerManager.isPowerSaveMode
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose { runCatching { context.unregisterReceiver(receiver) } }
        }
    }

    val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
    val blurResources = remember(supportsBlur, blurRadius, topHeight, bottomHeight) {
        if (supportsBlur) {
            val shader = RuntimeShader(progressiveBlurShader).apply {
                setFloatUniform("blurRadius", blurRadius)
                setFloatUniform("topHeight", topHeight)
                setFloatUniform("bottomHeight", bottomHeight)
            }
            shader to RenderEffect.createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        } else {
            null
        }
    }
    val blurModifier = if (blurResources != null && !powerSaveMode) {
        val (shader, effect) = blurResources
        Modifier.graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.setFloatUniform("contentHeight", size.height)
            }
            renderEffect = effect
        }
    } else {
        Modifier
    }

    val gradientModifier = Modifier.drawWithCache {
        val topBrush = Brush.verticalGradient(
            colors = listOf(overlayColor, Color.Transparent),
            endY = topHeight
        )
        val bottomStart = size.height - bottomHeight
        val bottomBrush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, overlayColor),
            startY = bottomStart,
            endY = size.height
        )
        onDrawWithContent {
            drawContent()
            if (topHeight > 0f) {
                drawRect(topBrush, size = Size(size.width, topHeight))
            }
            if (bottomHeight > 0f) {
                drawRect(
                    brush = bottomBrush,
                    topLeft = Offset(0f, bottomStart),
                    size = Size(size.width, bottomHeight)
                )
            }
        }
    }

    this.then(blurModifier).then(gradientModifier)
}
