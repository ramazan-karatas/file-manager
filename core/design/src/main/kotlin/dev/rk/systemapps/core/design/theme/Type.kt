package dev.rk.systemapps.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sistem yazı tipi bilinçli tercih: uygulamalar cihazın geri kalanıyla aynı görünmeli.
 * Yalnızca liste satırlarında okunabilirlik için satır yükseklikleri biraz artırıldı.
 */
internal val AppTypography = Typography().let { base ->
    base.copy(
        bodyLarge = base.bodyLarge.copy(lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
        titleMedium = base.titleMedium.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        ),
    )
}
