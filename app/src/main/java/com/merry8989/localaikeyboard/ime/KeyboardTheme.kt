package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Color

/** A colour scheme for the keyboard. */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val background: Int,
    val keyBackground: Int,
    val specialKeyBackground: Int,
    val keyText: Int,
    val accent: Int,
) {
    val isDark: Boolean get() = relativeLuminance(background) < 0.5

    private fun relativeLuminance(c: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(Color.red(c)) + 0.7152 * ch(Color.green(c)) + 0.0722 * ch(Color.blue(c))
    }
}

/** The 24 built-in themes. */
object ThemeRepository {

    private fun t(
        id: String, name: String,
        bg: String, key: String, special: String, text: String, accent: String,
    ) = KeyboardTheme(
        id = id, name = name,
        background = Color.parseColor(bg),
        keyBackground = Color.parseColor(key),
        specialKeyBackground = Color.parseColor(special),
        keyText = Color.parseColor(text),
        accent = Color.parseColor(accent),
    )

    val themes: List<KeyboardTheme> = listOf(
        t("midnight", "Midnight", "#11131A", "#2A2F3A", "#1C2029", "#F5F6FA", "#FF6B35"),
        t("daylight", "Daylight", "#F2F3F7", "#FFFFFF", "#E3E5EC", "#1A1C22", "#2F6BFF"),
        t("ocean", "Ocean", "#0B2545", "#13315C", "#0A1D37", "#EAF0F6", "#38BDF8"),
        t("forest", "Forest", "#0F1E14", "#1D3324", "#14271B", "#E8F0E9", "#4ADE80"),
        t("sunset", "Sunset", "#2A0E2E", "#4A1942", "#34102B", "#FDE8E8", "#FF8C42"),
        t("rose", "Rose", "#2B0F1A", "#4A1B2A", "#36101C", "#FFE9EF", "#FF5D8F"),
        t("monochrome", "Monochrome", "#000000", "#1C1C1C", "#101010", "#FFFFFF", "#FFFFFF"),
        t("neon", "Neon", "#0A0A12", "#16162A", "#101020", "#E0E0FF", "#39FF14"),
        t("cyberpunk", "Cyberpunk", "#0D0221", "#1B0B3B", "#12062B", "#F8F0FF", "#FF2E97"),
        t("pastel", "Pastel", "#FDF6F0", "#F7E1D7", "#EFD9CE", "#3A2E2A", "#E07A5F"),
        t("coffee", "Coffee", "#1E1611", "#33261D", "#271C15", "#F0E6DC", "#C08552"),
        t("slate", "Slate", "#1F2937", "#374151", "#111827", "#F9FAFB", "#60A5FA"),
        t("cherry", "Cherry", "#1A0A0A", "#3A1212", "#2A0D0D", "#FFECEC", "#EF4444"),
        t("mint", "Mint", "#E8F5EE", "#FFFFFF", "#CFE9DB", "#10331F", "#10B981"),
        t("lavender", "Lavender", "#221A33", "#342A4D", "#291F3D", "#F3EEFF", "#A78BFA"),
        t("amber", "Amber", "#2A1E08", "#3F2E10", "#33240C", "#FFF4E0", "#F59E0B"),
        t("steel", "Steel", "#20262E", "#333B45", "#171B21", "#E6EAF0", "#9AA5B1"),
        t("carbon", "Carbon", "#151515", "#262626", "#1B1B1B", "#EDEDED", "#00C2A8"),
        t("ice", "Ice", "#0E1B24", "#16303F", "#0B1620", "#E6F4FA", "#22D3EE"),
        t("ember", "Ember", "#1F0D06", "#3A1A0C", "#2A1208", "#FFEDE3", "#FF5722"),
        t("grape", "Grape", "#1B0E2A", "#2E1A47", "#221338", "#F2E9FF", "#C026D3"),
        t("sand", "Sand", "#F4ECD8", "#FFF9EC", "#E7DCC0", "#3A3324", "#B45309"),
        t("sky", "Sky", "#EAF4FF", "#FFFFFF", "#D6E8FB", "#0B2545", "#2563EB"),
        t("nordic", "Nordic", "#2E3440", "#3B4252", "#272C36", "#ECEFF4", "#88C0D0"),
        t("terminal", "Terminal", "#0C0F0A", "#16201A", "#0F1712", "#B8FFC4", "#22C55E"),
        t("bubblegum", "Bubblegum", "#FFEFF6", "#FFFFFF", "#FBD9E9", "#4A1230", "#FF4FA3"),
    )

    val default: KeyboardTheme get() = themes.first()

    fun byId(id: String?): KeyboardTheme = themes.firstOrNull { it.id == id } ?: default
}

/** Persists the chosen theme id. */
class ThemePrefs(context: Context) {

    private val sp = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)

    var themeId: String
        get() = sp.getString(KEY_THEME, ThemeRepository.default.id) ?: ThemeRepository.default.id
        set(value) {
            sp.edit().putString(KEY_THEME, value).apply()
        }

    var soundEnabled: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(value) = sp.edit().putBoolean(KEY_SOUND, value).apply()

    var vibrateEnabled: Boolean
        get() = sp.getBoolean(KEY_VIBRATE, true)
        set(value) = sp.edit().putBoolean(KEY_VIBRATE, value).apply()

    var autoCorrect: Boolean
        get() = sp.getBoolean(KEY_AUTOCORRECT, true)
        set(value) = sp.edit().putBoolean(KEY_AUTOCORRECT, value).apply()

    var autoCapitalize: Boolean
        get() = sp.getBoolean(KEY_AUTOCAP, true)
        set(value) = sp.edit().putBoolean(KEY_AUTOCAP, value).apply()

    var doubleSpacePeriod: Boolean
        get() = sp.getBoolean(KEY_DBLSPACE, true)
        set(value) = sp.edit().putBoolean(KEY_DBLSPACE, value).apply()

    var learnWords: Boolean
        get() = sp.getBoolean(KEY_LEARN, true)
        set(value) = sp.edit().putBoolean(KEY_LEARN, value).apply()

    // ---- key style -----------------------------------------------------------

    var cornerRadiusDp: Int
        get() = sp.getInt(KEY_CORNER, 8)
        set(value) = sp.edit().putInt(KEY_CORNER, value).apply()

    var outlineWidthDp: Int
        get() = sp.getInt(KEY_OUTLINE_W, 0)
        set(value) = sp.edit().putInt(KEY_OUTLINE_W, value).apply()

    var outlineColor: Int
        get() = sp.getInt(KEY_OUTLINE_C, 0x00000000)
        set(value) = sp.edit().putInt(KEY_OUTLINE_C, value).apply()

    var keyHeightDp: Int
        get() = sp.getInt(KEY_KEY_HEIGHT, 260)
        set(value) = sp.edit().putInt(KEY_KEY_HEIGHT, value).apply()

    fun theme(): KeyboardTheme = ThemeRepository.byId(themeId)

    fun keyStyle(): KeyStyle = KeyStyle(
        cornerRadiusDp = cornerRadiusDp,
        outlineWidthDp = outlineWidthDp,
        outlineColor = outlineColor,
        keyHeightDp = keyHeightDp,
    )

    private companion object {
        const val KEY_THEME = "theme_id"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_VIBRATE = "vibrate_enabled"
        const val KEY_AUTOCORRECT = "autocorrect_enabled"
        const val KEY_AUTOCAP = "autocapitalize_enabled"
        const val KEY_DBLSPACE = "double_space_period"
        const val KEY_LEARN = "learn_words"
        const val KEY_CORNER = "key_corner_radius"
        const val KEY_OUTLINE_W = "key_outline_width"
        const val KEY_OUTLINE_C = "key_outline_color"
        const val KEY_KEY_HEIGHT = "key_height"
    }
}
