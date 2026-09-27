package dev.rk.systemapps.core.design.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rk.systemapps.core.design.theme.SystemAppsTheme

/**
 * Hem dosya hem parça listelerinde kullanılan tek satır.
 * Domain tiplerini bilmez; yalnızca metin ve lambda alır (CLAUDE.md "Compose" kuralı).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    // Seçim rengi geçişli: uzun basışta satır sıçramadan doluyor.
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "AppListItemSelection",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Seçiliyken önizleme gizlenmiyor; onay rozeti köşeye bindiriliyor.
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            leading()
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        trailing()
    }
}

/** Dosya/klasör ikonu için varsayılan sarmalayıcı. */
@Composable
fun ItemIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(28.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Liste satırı — açık")
@Preview(name = "Liste satırı — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppListItemPreview() {
    SystemAppsTheme {
        Surface {
            Column {
                AppListItem(
                    title = "Belgeler",
                    subtitle = "24 öğe",
                    leading = { ItemIcon(Icons.Outlined.Folder) },
                )
                AppListItem(
                    title = "fatura_2026_şubat.pdf",
                    subtitle = "1,2 MB · 12 Şub 2026",
                    leading = { ItemIcon(Icons.AutoMirrored.Outlined.InsertDriveFile) },
                )
                AppListItem(
                    title = "seçili_dosya.txt",
                    subtitle = "4,0 KB",
                    selected = true,
                )
            }
        }
    }
}
