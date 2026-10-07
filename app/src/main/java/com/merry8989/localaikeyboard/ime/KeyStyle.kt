package com.merry8989.localaikeyboard.ime

/** How each key is drawn and how presses behave. */
data class KeyStyle(
    val cornerRadiusDp: Int = 8,
    val outlineWidthDp: Int = 0,
    val outlineColor: Int = 0x00000000,
    val keyHeightDp: Int = 260,
    val fontSizePercent: Int = 100,
    val keySpacingDp: Int = 3,
    val longPressDelayMs: Int = 300,
) {
    val hasOutline: Boolean get() = outlineWidthDp > 0
}
