package dev.rk.systemapps.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * Dynamic color desteklemeyen cihazlar için yedek palet.
 * Android 12+ cihazlarda (hedefimiz olan HyperOS dâhil) bunun yerine
 * duvar kâğıdından türetilen renkler kullanılır.
 */
internal val FallbackPrimaryLight = Color(0xFF2F6B4F)
internal val FallbackSecondaryLight = Color(0xFF4F6354)
internal val FallbackTertiaryLight = Color(0xFF3A656F)
internal val FallbackErrorLight = Color(0xFFBA1A1A)

internal val FallbackPrimaryDark = Color(0xFF96D5AF)
internal val FallbackSecondaryDark = Color(0xFFB6CCBA)
internal val FallbackTertiaryDark = Color(0xFFA2CDD9)
internal val FallbackErrorDark = Color(0xFFFFB4AB)
