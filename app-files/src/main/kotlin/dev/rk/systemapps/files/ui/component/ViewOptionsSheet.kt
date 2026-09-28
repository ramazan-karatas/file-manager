package dev.rk.systemapps.files.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.SortBy

/**
 * Görünüm ve sıralama tercihlerinin tek yeri (docs/files/SPEC.md §3.1, §3.2).
 *
 * Gezginde ve kategori ekranlarında aynı sayfa kullanılıyor. Kategori listesinde
 * klasör bulunmadığı için [onToggleFoldersFirst] ve [onToggleShowHidden] null
 * geçilebilir; o zaman ilgili satırlar çizilmez.
 *
 * Segment düğmeleri "durum" gösterdiği için geçiş eylemleri yalnızca seçim
 * gerçekten değiştiğinde gönderilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOptionsSheet(
    prefs: BrowserPrefs,
    onSetSortBy: (SortBy) -> Unit,
    onToggleSortDirection: () -> Unit,
    onToggleViewMode: () -> Unit,
    onDismiss: () -> Unit,
    onToggleFoldersFirst: (() -> Unit)? = null,
    onToggleShowHidden: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.options_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            )

            SectionLabel(stringResource(R.string.options_view))
            ViewModeSelector(gridMode = prefs.gridMode, onToggle = onToggleViewMode)

            SectionLabel(stringResource(R.string.options_sort_by))
            SortBySelector(current = prefs.sortBy, onSelect = onSetSortBy)

            SectionLabel(stringResource(R.string.options_sort_order))
            SortOrderSelector(ascending = prefs.ascending, onToggle = onToggleSortDirection)

            if (onToggleFoldersFirst != null || onToggleShowHidden != null) {
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            onToggleFoldersFirst?.let { toggle ->
                SwitchRow(
                    label = stringResource(R.string.sort_folders_first),
                    checked = prefs.foldersFirst,
                    onToggle = toggle,
                )
            }
            onToggleShowHidden?.let { toggle ->
                SwitchRow(
                    label = stringResource(R.string.sort_show_hidden),
                    checked = prefs.showHidden,
                    onToggle = toggle,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeSelector(gridMode: Boolean, onToggle: () -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        // Segment düğmesinin varsayılan onay ikonu yerine görünüm ikonu gösteriliyor;
        // iki ikon yan yana dar ekranda etiketi taşırıyordu.
        SegmentedButton(
            selected = !gridMode,
            onClick = { if (gridMode) onToggle() },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ViewList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = { Text(stringResource(R.string.action_view_list)) },
        )
        SegmentedButton(
            selected = gridMode,
            onClick = { if (!gridMode) onToggle() },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = { Text(stringResource(R.string.action_view_grid)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortBySelector(current: SortBy, onSelect: (SortBy) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortBy.entries.forEach { sortBy ->
            val selected = sortBy == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(sortBy) },
                label = { Text(stringResource(sortBy.labelRes())) },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOrderSelector(ascending: Boolean, onToggle: () -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        SegmentedButton(
            selected = ascending,
            onClick = { if (!ascending) onToggle() },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = { Text(stringResource(R.string.sort_ascending)) },
        )
        SegmentedButton(
            selected = !ascending,
            onClick = { if (ascending) onToggle() },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            label = { Text(stringResource(R.string.sort_descending)) },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun SortBy.labelRes(): Int = when (this) {
    SortBy.NAME -> R.string.sort_by_name
    SortBy.SIZE -> R.string.sort_by_size
    SortBy.DATE -> R.string.sort_by_date
    SortBy.TYPE -> R.string.sort_by_type
}
