package akihz.anlaki.dev.data

/** Tunable presentation values for the home screen's refresh-rate buttons. */
data class HomeDebugSettings(
    val showFakeRefreshRates: Boolean = false,
    val hzTextSizeSp: Float = 44f,
    val buttonHeightDp: Float = 160f,
    val buttonWidthPercent: Float = 100f,
    val buttonSpacingDp: Float = 12f,
    val restingCornerDp: Float = 8f,
    val selectedCornerDp: Float = 32f,
    val pressedCornerDp: Float = 20f
) {
    companion object {
        private val fakeRefreshRates = listOf(
            24f, 30f, 48f, 50f, 60f, 72f, 75f, 90f,
            100f, 120f, 144f, 165f, 240f, 360f
        )

        /** Returns the app's default home-screen tuning. */
        fun defaults(): HomeDebugSettings = HomeDebugSettings()

        /** Returns a dense preview list that retains every real device rate. */
        fun previewRates(supportedRates: List<Float>): List<Float> =
            fakeRefreshRates.fold(supportedRates.distinct()) { rates, fakeRate ->
                if (rates.any { kotlin.math.abs(it - fakeRate) < 0.01f }) rates else rates + fakeRate
            }.sorted()
    }
}
