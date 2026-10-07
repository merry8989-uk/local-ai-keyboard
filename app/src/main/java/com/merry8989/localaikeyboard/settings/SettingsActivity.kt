package com.merry8989.localaikeyboard.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.merry8989.localaikeyboard.App

/**
 * The app's front door (also the launcher activity). Sets up the keyboard as the
 * system IME and manages the on-device models.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private val deleteButtons = mutableListOf<Pair<ModelSpec, Button>>()

    private val models get() = App.instance.modelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }

        root.addView(title("Local AI Keyboard"))
        root.addView(body("An offline keyboard with on-device AI. Nothing you type ever leaves this phone."))

        root.addView(section("Set up"))
        root.addView(button("Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("Choose input method") {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        })

        root.addView(section("On-device AI model"))
        status = body("")
        root.addView(status)

        for (spec in ModelManager.AVAILABLE) {
            root.addView(button("Download ${spec.name} (${spec.approxLabel})") {
                val id = models.enqueueDownload(spec)
                if (id == -1L) {
                    toast("Download could not start.")
                } else {
                    toast("Downloading ${spec.name}. Reopen this screen when it finishes.")
                }
            })
            val del = button("Delete ${spec.name}") {
                models.delete(spec)
                refresh()
                toast("Deleted ${spec.name}.")
            }
            deleteButtons += spec to del
            root.addView(del)
        }

        root.addView(section("Privacy"))
        root.addView(
            body(
                "• No INTERNET permission in the keyboard process.\n" +
                    "• No clipboard access, no analytics, no logging of your text.\n" +
                    "• The model is fetched once, by the system downloader, on your command."
            )
        )

        return root
    }

    private fun refresh() {
        val installed = models.installedSpec()
        status.text = if (installed != null) {
            "Model ready: ${installed.name}. AI actions are available."
        } else {
            "No model installed — AI actions are disabled until you download one."
        }
        for ((spec, button) in deleteButtons) {
            button.isEnabled = models.isInstalled(spec)
        }
    }

    // ---- tiny view helpers ---------------------------------------------------

    private fun title(t: String) = TextView(this).apply {
        text = t; textSize = 24f; setPadding(0, 0, 0, dp(8))
    }

    private fun section(t: String) = TextView(this).apply {
        text = t; textSize = 16f; setPadding(0, dp(20), 0, dp(8))
    }

    private fun body(t: String) = TextView(this).apply {
        text = t; textSize = 15f; setPadding(0, 0, 0, dp(8))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        gravity = Gravity.START
        setOnClickListener { onClick() }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
