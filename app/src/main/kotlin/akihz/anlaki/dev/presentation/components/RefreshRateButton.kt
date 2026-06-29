package akihz.anlaki.dev.presentation.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RefreshRateButton(
    hz: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator

    if (isSelected) {
        Button(
            onClick = {
                performHapticFeedback(vibrator)
                onClick()
            },
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = "${hz.toInt()} Hz", fontWeight = FontWeight.Bold)
        }
    } else {
        FilledTonalButton(
            onClick = {
                performHapticFeedback(vibrator)
                onClick()
            },
            modifier = modifier.fillMaxWidth()
        ) {
            Text(text = "${hz.toInt()} Hz")
        }
    }
}

private fun performHapticFeedback(vibrator: Vibrator?) {
    vibrator?.let {
        if (!it.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(50)
        }
    }
}
