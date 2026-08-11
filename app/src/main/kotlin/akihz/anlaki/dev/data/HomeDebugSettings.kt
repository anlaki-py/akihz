package akihz.anlaki.dev.data

/** Tunable presentation values for the home screen's refresh-rate buttons. */
data class HomeDebugSettings(
    val hzTextSizeSp: Float = 28f,
    val buttonHeightDp: Float = 72f,
    val buttonWidthPercent: Float = 100f,
    val buttonSpacingDp: Float = 12f,
    val restingCornerDp: Float = 32f,
    val selectedCornerDp: Float = 20f,
    val pressedCornerDp: Float = 16f
) {
    companion object {
        /** Returns the app's default home-screen tuning. */
        fun defaults(): HomeDebugSettings = HomeDebugSettings()
    }
}
