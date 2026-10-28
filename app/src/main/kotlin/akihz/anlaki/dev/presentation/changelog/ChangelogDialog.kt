package akihz.anlaki.dev.presentation.changelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.data.changelog.BundledChangelog
import akihz.anlaki.dev.domain.changelog.Changelog
import akihz.anlaki.dev.domain.changelog.ChangelogBlock
import kotlinx.coroutines.CancellationException
import timber.log.Timber

private sealed interface ChangelogState {
    data object Loading : ChangelogState
    data object Failed : ChangelogState
    data class Ready(val changelog: Changelog) : ChangelogState
}

/** Shows [versionName]'s offline release notes, with access to the complete bundled history. */
@Composable
fun ChangelogDialog(versionName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val state by produceState<ChangelogState>(ChangelogState.Loading, context) {
        value = try {
            ChangelogState.Ready(BundledChangelog.load(context))
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.e(error, "Could not read bundled changelog")
            ChangelogState.Failed
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(showHistory) { listState.scrollToItem(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (showHistory) "Changelog" else "What's new")
                Text("Version $versionName", style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            when (val loaded = state) {
                ChangelogState.Loading -> Text("Loading release notes…")
                ChangelogState.Failed -> Text("Could not read the release notes. Close this window and try again.")
                is ChangelogState.Ready -> {
                    val current = loaded.changelog.forVersion(versionName)
                    val visible = if (showHistory) loaded.changelog.releases else listOfNotNull(current)
                    if (visible.isEmpty()) {
                        Text("No notes were included for this version. You can still read the full changelog.")
                    } else {
                        SelectionContainer {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                visible.forEach { release ->
                                    if (showHistory) {
                                        item {
                                            Text(
                                                text = if (release.version == "Unreleased" && release == current) {
                                                    versionName
                                                } else release.heading,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    items(release.blocks) { block -> ChangelogText(block) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            if (state is ChangelogState.Ready) {
                TextButton(onClick = { showHistory = !showHistory }) {
                    Text(if (showHistory) "This version" else "Full changelog")
                }
            }
        }
    )
}

@Composable
private fun ChangelogText(block: ChangelogBlock) {
    // The source uses backticks for version numbers and paths; normal text is easier to read here.
    val text = block.text.replace("`", "")
    when (block.kind) {
        ChangelogBlock.Kind.Heading -> Text(text, style = MaterialTheme.typography.titleSmall)
        ChangelogBlock.Kind.Paragraph -> Text(text, style = MaterialTheme.typography.bodyMedium)
        ChangelogBlock.Kind.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", style = MaterialTheme.typography.bodyMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
