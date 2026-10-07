package com.merry8989.localaikeyboard.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.merry8989.localaikeyboard.App

/**
 * The on-device user dictionary: words the keyboard has learned from your
 * typing, plus words you add by hand. Stored only on this device.
 */
class DictionaryActivity : AppCompatActivity() {

    private val dict get() = App.instance.userDictionary
    private lateinit var list: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        refresh()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply { text = "Personal dictionary"; textSize = 22f; setPadding(0, 0, 0, dp(8)) })
        root.addView(TextView(this).apply {
            text = "Words you type are learned here so suggestions improve. " +
                "Everything stays on this device."
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        val addRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        input = EditText(this).apply { hint = "Add a word" }
        addRow.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addRow.addView(Button(this).apply {
            text = "Add"
            setOnClickListener {
                val w = input.text.toString().trim()
                if (w.isNotEmpty()) {
                    dict.add(w, weight = 5)
                    input.setText("")
                    refresh()
                }
            }
        })
        root.addView(addRow)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(Button(this).apply {
            text = "Clear all learned words"
            setOnClickListener {
                dict.clear()
                refresh()
                toast("Dictionary cleared")
            }
        })

        return root
    }

    private fun refresh() {
        list.removeAllViews()
        val words = dict.all()
        if (words.isEmpty()) {
            list.addView(TextView(this).apply { text = "No learned words yet."; setPadding(0, dp(12), 0, 0) })
            return
        }
        for ((word, freq) in words) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
            }
            row.addView(TextView(this).apply { text = word; textSize = 16f },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply { text = "×$freq"; textSize = 13f })
            row.addView(Button(this).apply {
                text = "Delete"
                setOnClickListener { dict.remove(word); refresh() }
            })
            list.addView(row)
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
