package akihz.anlaki.dev.presentation.modifiers

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

internal enum class BlurDirection { Top, Bottom }

private val progressiveBlurShader = """
    uniform shader content;
    uniform float blurRadius;
    uniform float height;
    uniform float contentHeight;
    uniform int isTop;

    half4 main(float2 fragCoord) {
        float progress = isTop == 1
            ? 1.0 - clamp(fragCoord.y / height, 0.0, 1.0)
            : 1.0 - clamp((contentHeight - fragCoord.y) / height, 0.0, 1.0);
        progress = pow(progress, 1.5);
        float radius = progress * blurRadius;
        if (radius <= 0.0) return content.eval(fragCoord);

        half4 accum = half4(0.0);
        float weightSum = 0.0;
        float dither = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
        float2 jitter = float2(dither - 0.5, fract(dither * 1.618) - 0.5);
        const int samples = 4;
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
 * Applies Essentials-style progressive edge blur with a gradient fallback.
 */
internal fun Modifier.progressiveBlur(
    blurRadius: Float,
    height: Float,
    direction: BlurDirection
): Modifier = composed {
    val context = LocalContext.current
    val powerSaveMode = remember(context) {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }
    val problematicSamsung = remember {
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }
    val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
    val blurModifier =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            blurRadius > 0f && !powerSaveMode && !problematicSamsung
        ) {
            Modifier.graphicsLayer {
                val shader = RuntimeShader(progressiveBlurShader)
                shader.setFloatUniform("blurRadius", blurRadius)
                shader.setFloatUniform("height", height)
                shader.setFloatUniform("contentHeight", size.height)
                shader.setIntUniform("isTop", if (direction == BlurDirection.Top) 1 else 0)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
        } else {
            Modifier
        }

    val gradientModifier = Modifier.drawWithContent {
        drawContent()
        val brush = when (direction) {
            BlurDirection.Top -> Brush.verticalGradient(
                colors = listOf(overlayColor, Color.Transparent),
                endY = height
            )
            BlurDirection.Bottom -> Brush.verticalGradient(
                colors = listOf(Color.Transparent, overlayColor),
                startY = size.height - height
            )
        }
        drawRect(brush = brush)
    }

    this.then(blurModifier).then(gradientModifier)
}
