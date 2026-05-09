package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Scaffold layout for preference/settings screens with a top app bar and scrollable content.
 *
 * @param label Title displayed in the top app bar
 * @param backArrowVisible Whether to show the back arrow
 * @param onBackClick Called when back arrow is pressed
 * @param actions Additional actions in the top bar
 * @param content The screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceLayout(
    label: String,
    modifier: Modifier = Modifier,
    backArrowVisible: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (backArrowVisible) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = actions
            )
        }
    ) { padding ->
        PreferenceColumn(
            contentPadding = padding,
            content = content
        )
    }
}

/**
 * Scrollable column with consistent spacing and bottom padding for preference content.
 *
 * @param contentPadding Padding from the scaffold
 * @param verticalArrangement Spacing between children
 * @param content The column content
 */
@Composable
private fun PreferenceColumn(
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.Start
    ) {
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}
