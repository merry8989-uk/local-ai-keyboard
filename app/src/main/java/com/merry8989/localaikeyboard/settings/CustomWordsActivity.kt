package com.merry8989.localaikeyboard.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
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
import com.merry8989.localaikeyboard.spell.CustomWordsStore

/**
 * Per-key custom long-press words for one language. Each key gets a text field;
 * enter words separated by commas.
 */
class CustomWordsActivity : AppCompatActivity() {

    private lateinit var store: CustomWordsStore
    private lateinit var langId: String
    private val editors = mutableListOf<Pair<String, EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = App.instance.customWordsStore
        langId = intent.getStringExtra(EXTRA_LANG_ID) ?: "en"
        val langName = intent.getStringExtra(EXTRA_LANG_NAME) ?: "English"
        setContentView(buildUi(langName))
    }

    override fun onPause() {
        saveAll()
        super.onPause()
    }

    private fun buildUi(langName: String): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "Custom Long Press Words"; textSize = 22f; setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "$langName — enter words separated by commas"
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        for (c in 'a'..'z') {
            val key = c.toString()
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }

            row.addView(TextView(this).apply {
                text = key
                gravity = Gravity.CENTER
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFF2F6BFF.toInt())
                }
            }, LinearLayout.LayoutParams(dp(40), dp(40)))

            val field = EditText(this).apply {
                hint = "Custom Words: Enter words separated by commas"
                inputType = InputType.TYPE_CLASS_TEXT
                setText(store.words(langId, key).joinToString(", "))
                textSize = 15f
            }
            row.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(12), 0, 0, 0)
            })

            editors += key to field
            root.addView(row)
        }

        root.addView(Button(this).apply {
            text = "Save"
            setOnClickListener {
                saveAll()
                toast("Saved")
            }
        })
        root.addView(Button(this).apply {
            text = "Clear all for this language"
            setOnClickListener {
                store.clear(langId)
                for ((_, field) in editors) field.setText("")
                toast("Cleared")
            }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun saveAll() {
        for ((key, field) in editors) {
            store.setWords(langId, key, field.text.toString().split(","))
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_LANG_ID = "lang_id"
        const val EXTRA_LANG_NAME = "lang_name"
    }
}
