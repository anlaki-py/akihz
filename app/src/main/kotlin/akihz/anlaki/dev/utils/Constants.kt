package akihz.anlaki.dev.utils

/**
 * App-wide constants.
 */
object Constants {
    /** Broadcast action sent when refresh rate changes. */
    const val ACTION_RATE_CHANGED = "akihz.anlaki.dev.RATE_CHANGED"

    /** Extra key for rate value in broadcasts. */
    const val EXTRA_RATE = "rate"

    /** Extra key for error type in broadcasts. */
    const val EXTRA_ERROR_TYPE = "error_type"

    /** Extra key for error message in broadcasts. */
    const val EXTRA_ERROR_MESSAGE = "error_message"

    /** Notification channel ID for the keep-alive service. */
    const val CHANNEL_ID = "akihz_refresh_rate_channel"

    /** Notification ID for the keep-alive service. */
    const val NOTIFICATION_ID = 1001
}