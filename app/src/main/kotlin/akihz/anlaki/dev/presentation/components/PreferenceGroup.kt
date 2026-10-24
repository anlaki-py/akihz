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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import akihz.anlaki.dev.presentation.theme.AkihzColors

private val GroupCornerRadius = 20.dp
private val GroupInnerCornerRadius = 4.dp
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

/** Collects the rows and form blocks that make up one preference group. */
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
    fun formItem(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    )
}

private class PreferenceGroupScopeImpl : PreferenceGroupScope {
    val items = mutableListOf<PreferenceItem>()

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
        items += PreferenceItem(
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
    }

    override fun formItem(
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        items += PreferenceItem(
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
    }
}

/**
 * Groups related settings beneath a section label.
 *
 * Every row is its own card on `surfaceBright`, with 20dp outer corners, 4dp
 * inner corners, and a 2dp gap. The gap is what keeps rows from reading as one
 * merged block.
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
    scope.content()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
        )

        val count = scope.items.size
        scope.items.forEachIndexed { index, item ->
            if (item.formContent != null) {
                PreferenceFormItem(
                    item = item,
                    shape = preferenceItemShape(index, count)
                )
            } else {
                PreferenceListItem(item = item, index = index, count = count)
            }
            if (index != count - 1) {
                Spacer(modifier = Modifier.height(GroupItemSpacing))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun preferenceItemShape(index: Int, count: Int): Shape {
    val top = if (index == 0) GroupCornerRadius else GroupInnerCornerRadius
    val bottom = if (index == count - 1) GroupCornerRadius else GroupInnerCornerRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun PreferenceFormItem(item: PreferenceItem, shape: Shape) {
    Surface(
        modifier = item.modifier.fillMaxWidth(),
        shape = shape,
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
private fun PreferenceListItem(item: PreferenceItem, index: Int, count: Int) {
    val isFirst = index == 0
    val isLast = index == count - 1
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val toggleState = item.toggleState
    val onToggle = item.onToggle

    val topCorner by animateDpAsState(
        targetValue = if (isPressed || count == 1 || isFirst) {
            GroupCornerRadius
        } else {
            GroupInnerCornerRadius
        },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "preference top corner"
    )
    val bottomCorner by animateDpAsState(
        targetValue = if (isPressed || count == 1 || isLast) {
            GroupCornerRadius
        } else {
            GroupInnerCornerRadius
        },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "preference bottom corner"
    )

    ListItem(
        headlineContent = item.headlineContent ?: {},
        modifier = item.modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = topCorner,
                    topEnd = topCorner,
                    bottomStart = bottomCorner,
                    bottomEnd = bottomCorner
                )
            )
            .then(
                when {
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
