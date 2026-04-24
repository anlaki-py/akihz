package akihz.anlaki.dev.utils

object Constants {
const val ACTION_RATE_CHANGED = "akihz.anlaki.dev.RATE_CHANGED"
const val ACTION_UPDATE_TILE = "akihz.anlaki.dev.UPDATE_TILE"
const val ACTION_CYCLE_RATE = "akihz.anlaki.dev.CYCLE_RATE"
const val ACTION_ERROR = "akihz.anlaki.dev.ERROR"
const val EXTRA_RATE = "rate"
const val EXTRA_ERROR_TYPE = "error_type"
const val EXTRA_ERROR_MESSAGE = "error_message"

const val CHANNEL_ID = "akihz_refresh_rate_channel"
const val NOTIFICATION_ID = 1001

@Deprecated("Dynamic detection via DisplayManagerDataSource replaces hardcoded rates")
val REFRESH_RATES = listOf(60f, 90f, 120f)
}
