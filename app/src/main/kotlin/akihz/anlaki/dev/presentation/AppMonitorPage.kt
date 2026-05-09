package akihz.anlaki.dev.presentation

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import akihz.anlaki.dev.data.AppListRepository
import akihz.anlaki.dev.utils.AppMonitorService
import akihz.anlaki.dev.utils.BatteryOptimizationHelper
import akihz.anlaki.dev.utils.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dedicated page for managing per-app refresh rate profiles.
 *
 * Features:
 * - Search installed apps by name or package
 * - Filter: All / User apps / System apps
 * - Tap an app to set its refresh rate from supported rates
 * - Shows current profile rate inline
 * - Async loading with progress indicator
 * - Launcher apps shown first, sorted alphabetically
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMonitorPage(
    supportedRates: List<Float>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AppListRepository(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppListRepository.Filter.ALL) }
    var selectedApp by remember { mutableStateOf<AppListRepository.AppInfo?>(null) }
    var apps by remember { mutableStateOf<List<AppListRepository.AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isAccessibilityEnabled = remember { AppMonitorService.isEnabled(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load apps asynchronously on IO dispatcher
    LaunchedEffect(selectedFilter) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            repository.getApps(filter = selectedFilter, query = "")
        }
        apps = loaded
        isLoading = false
    }

    // Filter locally on main thread for search (fast enough for in-memory list)
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Monitor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = !isAccessibilityEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AccessibilityWarningCard()
            }

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FilterRow(
                selected = selectedFilter,
                onSelect = {
                    selectedFilter = it
                    searchQuery = ""
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Text(
                text = "${filteredApps.size} apps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (isLoading) {
                LoadingState()
            } else if (filteredApps.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val profileRate = PreferencesHelper.getAppProfile(app.packageName)
                        AppListItem(
                            app = app,
                            profileRate = profileRate,
                            onClick = { selectedApp = app }
                        )
                    }
                }
            }
        }
    }

    if (selectedApp != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null },
            sheetState = sheetState
        ) {
            RatePickerSheet(
                app = selectedApp!!,
                supportedRates = supportedRates,
                onRateSelected = { rate ->
                    PreferencesHelper.setAppProfile(selectedApp!!.packageName, rate)
                    selectedApp = null
                },
                onClear = {
                    PreferencesHelper.removeAppProfile(selectedApp!!.packageName)
                    selectedApp = null
                }
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading apps...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No apps found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun AccessibilityWarningCard() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Accessibility service required",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enable akihz in Accessibility settings for per-app profiles to work.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = { AppMonitorService.openSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }
            OutlinedButton(
                onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow background activity")
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search apps...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterRow(
    selected: AppListRepository.Filter,
    onSelect: (AppListRepository.Filter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == AppListRepository.Filter.ALL,
            onClick = { onSelect(AppListRepository.Filter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selected == AppListRepository.Filter.USER,
            onClick = { onSelect(AppListRepository.Filter.USER) },
            label = { Text("User") }
        )
        FilterChip(
            selected = selected == AppListRepository.Filter.SYSTEM,
            onClick = { onSelect(AppListRepository.Filter.SYSTEM) },
            label = { Text("System") }
        )
    }
}

@Composable
private fun AppListItem(
    app: AppListRepository.AppInfo,
    profileRate: Float?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        loadAppIcon(context, app.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(icon = icon, modifier = Modifier.size(44.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (profileRate != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${profileRate.toInt()} Hz",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }

    HorizontalDivider()
}

@Composable
private fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon != null) {
        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun loadAppIcon(context: Context, packageName: String): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun RatePickerSheet(
    app: AppListRepository.AppInfo,
    supportedRates: List<Float>,
    onRateSelected: (Float) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val currentRate = PreferencesHelper.getAppProfile(app.packageName)
    val icon = remember(app.packageName) {
        loadAppIcon(context, app.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIcon(icon = icon, modifier = Modifier.size(48.dp))
            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Text(
            text = if (currentRate != null) "Current: ${currentRate.toInt()} Hz" else "No profile set",
            style = MaterialTheme.typography.bodyMedium,
            color = if (currentRate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Select refresh rate",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        if (supportedRates.isEmpty()) {
            Text(
                text = "No supported rates detected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supportedRates.forEach { hz ->
                    val isSelected = currentRate != null && kotlin.math.abs(hz - currentRate) < 1f
                    OutlinedButton(
                        onClick = { onRateSelected(hz) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${hz.toInt()} Hz",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (currentRate != null) {
            TextButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear profile", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}