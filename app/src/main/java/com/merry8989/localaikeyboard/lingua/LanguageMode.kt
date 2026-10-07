package com.merry8989.localaikeyboard.lingua

/**
 * Which dictionary mix to prefer when ranking suggestions.
 * AUTO blends both and lets frequency decide.
 */
enum class LanguageMode(val tag: String) {
    AUTO("Auto"),
    ENGLISH("EN"),
    HINGLISH("HI");

    fun next(): LanguageMode = when (this) {
        AUTO -> ENGLISH
        ENGLISH -> HINGLISH
        HINGLISH -> AUTO
    }
}
