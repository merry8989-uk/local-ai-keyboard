package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.merry8989.localaikeyboard.stickers.StickerStore
import java.io.File

/**
 * The in-keyboard sticker strip. Shows the local sticker library; tapping one
 * copies it to the clipboard (a keyboard can't type an image into a text field).
 */
class StickerPanel(
    context: Context,
    private val store: StickerStore,
    private val onPick: (File) -> Unit,
    private val onManage: () -> Unit,
) : LinearLayout(context) {

    private val row = LinearLayout(context)
    private var theme: KeyboardTheme = ThemeRepository.default

    init {
        orientation = VERTICAL
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val scroller = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun show(theme: KeyboardTheme) {
        this.theme = theme
        setBackgroundColor(theme.background)
        refresh()
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isShowing() = visibility == View.VISIBLE

    fun refresh() {
        row.removeAllViews()
        val items = store.list()
        if (items.isEmpty()) {
            row.addView(label("No stickers yet — tap ＋ to add photos, GIFs or PNGs"))
        } else {
            for (f in items.take(30)) row.addView(thumb(f))
        }
        row.addView(chip("＋", onManage))
    }

    private fun thumb(file: File): View {
        val size = dp(64)
        return ImageView(context).apply {
            val bmp = loadThumb(file, size)
            if (bmp != null) setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                setColor(theme.keyBackground)
                cornerRadius = dp(8).toFloat()
            }
            val m = dp(4)
            layoutParams = LayoutParams(size, size).apply { setMargins(m, m, m, m) }
            isClickable = true
            setOnClickListener { onPick(file) }
        }
    }

    private fun chip(text: String, onClick: () -> Unit): TextView = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(theme.accent)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        typeface = Typeface.DEFAULT
        background = GradientDrawable().apply {
            setColor(theme.keyBackground)
            cornerRadius = dp(8).toFloat()
        }
        val m = dp(4)
        layoutParams = LayoutParams(dp(64), dp(64)).apply { setMargins(m, m, m, m) }
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun label(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(theme.keyText)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    private fun loadThumb(file: File, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
