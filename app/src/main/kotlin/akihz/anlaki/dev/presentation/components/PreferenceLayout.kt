@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package akihz.anlaki.dev.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.theme.AkihzColors

/**
 * Scaffold with a collapsing expressive top app bar and scrollable content.
 *
 * @param label screen title shown in the top app bar
 * @param modifier layout modifier supplied by the parent
 * @param navigationIcon optional icon displayed before the title
 * @param navigationContentDescription accessibility label for [navigationIcon]
 * @param onNavigationClick invoked when the navigation icon is selected
 * @param content preference groups to display
 */
@Composable
fun PreferenceLayout(
    label: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AkihzColors.pageContainer,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = navigationContentDescription
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = AkihzColors.topBarColors
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            content()
            // Keep the final row reachable while content still scrolls behind the floating toolbar.
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}
