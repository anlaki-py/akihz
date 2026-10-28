package akihz.anlaki.dev.data.changelog

import android.content.Context
import akihz.anlaki.dev.domain.changelog.Changelog
import akihz.anlaki.dev.domain.changelog.parseChangelog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads this build's complete changelog without a network request. */
object BundledChangelog {
    /** Reads the packaged Markdown on an IO thread; failures propagate to the reader UI. */
    suspend fun load(context: Context): Changelog = withContext(Dispatchers.IO) {
        val markdown = context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
        parseChangelog(markdown).also { check(it.releases.isNotEmpty()) { "Bundled changelog has no releases" } }
    }
}
