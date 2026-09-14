package akihz.anlaki.dev.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Shared notification channel setup.
 *
 * Every helper used to build its own channel and fetch the manager inline.
 * Channel ids, names, and importance stay with each helper. This file owns
 * only the repeated create call.
 */
internal object NotificationChannels {

    /** Creates or updates the channel for [id]. */
    fun ensure(
        context: Context,
        id: String,
        name: String,
        description: String,
        importance: Int,
        configure: NotificationChannel.() -> Unit = {}
    ) {
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
            configure()
        }
        manager(context).createNotificationChannel(channel)
    }

    /** System notification manager. */
    fun manager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
