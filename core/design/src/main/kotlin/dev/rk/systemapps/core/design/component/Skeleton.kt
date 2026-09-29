package dev.rk.systemapps.core.design.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rk.systemapps.core.design.theme.SystemAppsTheme

/**
 * Yükleme sırasında içeriğin yerine geçen gri kutular ("skeleton screen").
 *
 * Spinner yerine bunu göstermenin sebebi, gelecek içeriğin düzenini önceden
 * göstermesi ve beklemeyi daha kısa hissettirmesi. Kutular gerçek satırlarla
 * aynı ölçülerde ki liste geldiğinde zıplama olmasın.
 */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = DEFAULT_LIST_ITEMS,
) {
    val brush = rememberShimmerBrush()

    Column(modifier = modifier.fillMaxSize().skeletonSemantics()) {
        repeat(itemCount) { index ->
            SkeletonRow(brush = brush, titleFraction = titleFractionFor(index))
        }
    }
}

/** Izgara görünümünün karşılığı; [minTileSize] gerçek ızgarayla aynı verilmeli. */
@Composable
fun SkeletonGrid(
    minTileSize: Dp,
    modifier: Modifier = Modifier,
    itemCount: Int = DEFAULT_GRID_ITEMS,
) {
    val brush = rememberShimmerBrush()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .skeletonSemantics(),
    ) {
        // Gerçek ızgarayla aynı sütun sayısını bulmak için: LazyVerticalGrid'in
        // Adaptive hesabı da kullanılabilir genişliği minTileSize'a bölüyor.
        val columns = ((maxWidth + GRID_SPACING) / (minTileSize + GRID_SPACING))
            .toInt()
            .coerceAtLeast(1)

        Column(verticalArrangement = Arrangement.spacedBy(GRID_SPACING)) {
            repeat((itemCount + columns - 1) / columns) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
                ) {
                    repeat(columns) { column ->
                        SkeletonTile(
                            brush = brush,
                            titleFraction = titleFractionFor(row * columns + column),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow(brush: Brush, titleFraction: Float) {
    // Ölçüler AppListItem ile birebir: liste gelince satırlar yerinden oynamıyor.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brush),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBar(brush = brush, widthFraction = titleFraction, height = 14.dp)
            SkeletonBar(brush = brush, widthFraction = 0.35f, height = 10.dp)
        }
    }
}

@Composable
private fun SkeletonTile(brush: Brush, titleFraction: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush),
        )
        Box(modifier = Modifier.padding(top = 12.dp)) {
            SkeletonBar(brush = brush, widthFraction = titleFraction, height = 10.dp)
        }
    }
}

@Composable
private fun SkeletonBar(brush: Brush, widthFraction: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(brush),
    )
}

/**
 * Soldan sağa süzülen parlama ("shimmer"). Cihazda animasyonlar kapalıysa
 * sonsuz döngü kurulmuyor, düz renk dönüyor.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    if (!animationsEnabled()) return SolidColor(base)

    val density = LocalDensity.current
    val bandPx = with(density) { SHIMMER_BAND.toPx() }
    val widthPx = LocalWindowInfo.current.containerSize.width.toFloat().coerceAtLeast(bandPx)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -bandPx,
        targetValue = widthPx + bandPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
        ),
        label = "shimmerOffset",
    )

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset - bandPx, 0f),
        end = Offset(offset, 0f),
    )
}

/** Geliştirici seçeneklerinden ya da pil tasarrufundan animasyon kapatılmış olabilir. */
@Composable
private fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) > 0f
    }
}

/** Yer tutucular ekran okuyucuya okunmaz; anlamlı bir içerikleri yok. */
private fun Modifier.skeletonSemantics(): Modifier = clearAndSetSemantics {}

/** Satırlar aynı uzunlukta olmasın diye dönüşümlü genişlikler. */
private fun titleFractionFor(index: Int): Float = TITLE_FRACTIONS[index % TITLE_FRACTIONS.size]

private val TITLE_FRACTIONS = listOf(0.7f, 0.45f, 0.85f, 0.6f, 0.75f)
private val SHIMMER_BAND = 220.dp
private val GRID_SPACING = 4.dp
private const val SHIMMER_DURATION_MS = 1_200
private const val DEFAULT_LIST_ITEMS = 9
private const val DEFAULT_GRID_ITEMS = 18

@Preview(name = "İskelet — liste")
@Preview(name = "İskelet — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SkeletonListPreview() {
    SystemAppsTheme {
        SkeletonList(itemCount = 5)
    }
}

@Preview(name = "İskelet — ızgara")
@Composable
private fun SkeletonGridPreview() {
    SystemAppsTheme {
        SkeletonGrid(minTileSize = 104.dp, itemCount = 9)
    }
}
