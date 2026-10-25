package akihz.anlaki.dev.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.theme.AkihzColors

private val GroupItemSpacing = 2.dp

private class PreferenceItem(
    val modifier: Modifier,
    val onClick: (() -> Unit)?,
    val toggleState: Boolean?,
    val onToggle: ((Boolean) -> Unit)?,
    val headlineContent: (@Composable () -> Unit)?,
    val supportingContent: (@Composable () -> Unit)?,
    val leadingContent: (@Composable () -> Unit)?,
    val trailingContent: (@Composable () -> Unit)?,
    val formContent: (@Composable ColumnScope.() -> Unit)?
)

/** Restricts preference group calls to the nearest group scope. */
@DslMarker
annotation class PreferenceGroupDsl

/** Adds rows and form blocks directly to the surrounding preference group. */
@PreferenceGroupDsl
interface PreferenceGroupScope {
    /**
     * Adds a tappable row.
     *
     * @param modifier layout modifier for the row
     * @param onClick invoked when the row is tapped
     * @param toggleState switch state when the row toggles a value
     * @param onToggle invoked when the row or its switch changes
     * @param headlineContent primary row text
     * @param supportingContent optional description below the headline
     * @param leadingContent optional icon before the text
     * @param trailingContent optional control after the text
     */
    @Composable
    fun item(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        toggleState: Boolean? = null,
        onToggle: ((Boolean) -> Unit)? = null,
        headlineContent: (@Composable () -> Unit)? = null,
        supportingContent: (@Composable () -> Unit)? = null,
        leadingContent: (@Composable () -> Unit)? = null,
        trailingContent: (@Composable () -> Unit)? = null
    )

    /**
     * Adds a card block for controls that are not simple rows.
     *
     * @param modifier layout modifier for the card
     * @param content controls shown inside the card
     */
    @Composable
    fun formItem(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    )
}

private class PreferenceGroupScopeImpl : PreferenceGroupScope {
    @Composable
    override fun item(
        modifier: Modifier,
        onClick: (() -> Unit)?,
        toggleState: Boolean?,
        onToggle: ((Boolean) -> Unit)?,
        headlineContent: (@Composable () -> Unit)?,
        supportingContent: (@Composable () -> Unit)?,
        leadingContent: (@Composable () -> Unit)?,
        trailingContent: (@Composable () -> Unit)?
    ) {
        PreferenceListItem(
            item = PreferenceItem(
                modifier = modifier,
                onClick = onClick,
                toggleState = toggleState,
                onToggle = onToggle,
                headlineContent = headlineContent,
                supportingContent = supportingContent,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                formContent = null
            )
        )
    }

    @Composable
    override fun formItem(
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        PreferenceFormItem(
            item = PreferenceItem(
                modifier = modifier,
                onClick = null,
                toggleState = null,
                onToggle = null,
                headlineContent = null,
                supportingContent = null,
                leadingContent = null,
                trailingContent = null,
                formContent = content
            )
        )
    }
}

/**
 * Groups related settings beneath a section label.
 *
 * Rows stack into one block on `surfaceBright` with 20dp outer corners and 4dp
 * inner corners. A 2dp gap separates the rows. Pressing a row rounds it to the
 * full 20dp so the row reads as lifted.
 *
 * The rows sit inside a clipped column, which is what gives the first and last
 * row their outer corners without the group having to know its own row count.
 *
 * @param heading label describing the preference group
 * @param modifier layout modifier supplied by the caller
 * @param content rows and form blocks displayed in the group
 */
@Composable
fun PreferenceGroup(
    heading: String,
    modifier: Modifier = Modifier,
    content: @Composable PreferenceGroupScope.() -> Unit
) {
    val scope = PreferenceGroupScopeImpl()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GroupOuterCornerRadius)),
            verticalArrangement = Arrangement.spacedBy(GroupItemSpacing)
        ) {
            scope.content()
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PreferenceFormItem(item: PreferenceItem) {
    Surface(
        modifier = item.modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GroupInnerCornerRadius),
        color = AkihzColors.listItemColors.containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = item.formContent!!
        )
    }
}

@Composable
private fun PreferenceListItem(item: PreferenceItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val toggleState = item.toggleState
    val onToggle = item.onToggle
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) GroupOuterCornerRadius else GroupInnerCornerRadius,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "preference corner"
    )

    ListItem(
        headlineContent = item.headlineContent ?: {},
        modifier = item.modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius,
                    bottomStart = cornerRadius,
                    bottomEnd = cornerRadius
                )
            )
            .then(
                when {
                    // The row is the sole gesture target. A nested interactive Switch can
                    // consume the pointer event before the row's state callback runs.
                    toggleState != null && onToggle != null -> Modifier.toggleable(
                        value = toggleState,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Switch,
                        onValueChange = onToggle
                    )
                    item.onClick != null -> Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = item.onClick
                    )
                    else -> Modifier
                }
            ),
        supportingContent = item.supportingContent,
        leadingContent = item.leadingContent,
        trailingContent = item.trailingContent,
        colors = AkihzColors.listItemColors
    )
}
