package dev.rk.systemapps.files.ui.browser

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.rk.systemapps.core.design.component.ItemIcon
import dev.rk.systemapps.files.data.image.ApkIconRequest
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.FileNode
import java.io.File
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext

@Composable
fun Breadcrumbs(
    crumbs: List<Crumb>,
    onCrumbClick: (Crumb) -> Unit,
    modifier: Modifier = Modifier,
) {
    val storageRootLabel = stringResource(R.string.storage_internal)
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            crumbs.forEachIndexed { index, crumb ->
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                val isLast = index == crumbs.lastIndex
                TextButton(onClick = { onCrumbClick(crumb) }) {
                    Text(
                        text = if (crumb.isStorageRoot) storageRootLabel else crumb.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLast) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
    }
}

/**
 * Görsel, video ve APK için dosyanın kendisinden önizleme üretir; diğerlerinde
 * tür ikonu gösterir. Önizleme üretilemezse sessizce ikona düşer.
 */
@Composable
fun FileThumbnail(
    node: FileNode,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 6.dp,
) {
    val kind = node.kind()
    val iconPainter = rememberVectorPainter(kind.icon())

    if (!kind.hasThumbnail) {
        ItemIcon(icon = kind.icon(), modifier = modifier)
        return
    }

    val model = remember(node.id, kind) {
        if (kind == FileKind.APK) ApkIconRequest(node.id) else File(node.id)
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(model).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = iconPainter,
        error = iconPainter,
        fallback = iconPainter,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            // Beyaz/açık renkli görseller açık zeminde kaybolmasın diye ince çerçeve.
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(cornerRadius),
            ),
    )
}
