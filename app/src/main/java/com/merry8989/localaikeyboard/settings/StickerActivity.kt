package com.merry8989.localaikeyboard.settings

import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.merry8989.localaikeyboard.stickers.StickerStore
import java.io.File

/**
 * Manage the local sticker / GIF / photo library: import from the device,
 * copy to clipboard, share, or delete. Everything stays on the device.
 */
class StickerActivity : AppCompatActivity() {

    private lateinit var store: StickerStore
    private lateinit var grid: LinearLayout

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris: List<Uri> ->
        var added = 0
        for (uri in uris) if (store.import(uri) != null) added++
        toast("Added $added item(s)")
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = StickerStore(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Stickers & media"
            textSize = 22f
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Add photos, GIFs or PNGs. They are stored only on this device. " +
                "A keyboard can't type an image into a text field, so tapping a sticker " +
                "copies it to the clipboard to paste into a chat app."
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        root.addView(Button(this).apply {
            text = "Add images"
            gravity = Gravity.START
            setOnClickListener {
                pickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            }
        })

        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(grid) }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        return root
    }

    private fun refresh() {
        grid.removeAllViews()
        val items = store.list()
        if (items.isEmpty()) {
            grid.addView(TextView(this).apply {
                text = "No stickers yet."
                setPadding(0, dp(16), 0, 0)
            })
            return
        }
        var row: LinearLayout? = null
        items.forEachIndexed { index, file ->
            if (index % 3 == 0) {
                row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                grid.addView(row)
            }
            row!!.addView(cell(file), LinearLayout.LayoutParams(0, dp(110), 1f))
        }
    }

    private fun cell(file: File): View {
        val size = dp(100)
        return ImageView(this).apply {
            val bmp = loadThumb(file, size)
            if (bmp != null) setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                setColor(0xFF222222.toInt())
                cornerRadius = dp(8).toFloat()
            }
            val m = dp(6)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(m, m, m, m) }
            isClickable = true
            setOnClickListener {
                store.copyToClipboard(file)
                toast("Copied — paste it in your chat app")
            }
            setOnLongClickListener {
                confirmDelete(file)
                true
            }
        }
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete this sticker?")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(file)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadThumb(file: File, target: Int) = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) {
            sample *= 2
        }
        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
