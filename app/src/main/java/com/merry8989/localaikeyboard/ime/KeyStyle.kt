package com.merry8989.localaikeyboard.ime

/** How each key is drawn: corner rounding, outline, and overall height. */
data class KeyStyle(
    val cornerRadiusDp: Int = 8,
    val outlineWidthDp: Int = 0,
    val outlineColor: Int = 0x00000000,
    val keyHeightDp: Int = 260,
) {
    val hasOutline: Boolean get() = outlineWidthDp > 0
}
