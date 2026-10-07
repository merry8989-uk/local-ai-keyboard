package com.merry8989.localaikeyboard.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Owns the on-device model file: where it lives, whether it's present, how to
 * fetch it, and how to delete it.
 *
 * The model is downloaded by the system DownloadManager (a separate process),
 * which is why the keyboard itself needs no INTERNET permission.
 */
class ModelManager(private val context: Context) {

    val modelsDir: File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "models").apply { mkdirs() }

    val modelFile: File = File(modelsDir, MODEL_FILENAME)

    fun isInstalled(): Boolean = modelFile.exists() && modelFile.length() > MIN_BYTES

    fun modelPath(): String? = if (isInstalled()) modelFile.absolutePath else null

    fun delete(): Boolean = modelFile.delete()

    /**
     * Start the one-time model download. Returns the DownloadManager request id,
     * or -1 if the URL has not been configured yet.
     */
    fun enqueueDownload(): Long {
        if (!MODEL_URL.startsWith("http")) return -1L
        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("Local AI Keyboard model")
            .setDescription(MODEL_FILENAME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "models/$MODEL_FILENAME")
        val dm = context.getSystemService(DownloadManager::class.java) ?: return -1L
        return dm.enqueue(request)
    }

    companion object {
        const val MODEL_FILENAME = "gemma-3-1b-it.task"

        /**
         * TODO: set this to a URL for the model bundle you are licensed to ship.
         * Model hosting and licensing are your responsibility — the app only
         * fetches a file you point it at. See the MediaPipe LLM Inference docs
         * for the current Gemma `.task` / `.litertlm` bundle format.
         */
        const val MODEL_URL = "REPLACE_WITH_MODEL_URL"

        /** Anything smaller than this is a failed/partial download. */
        private const val MIN_BYTES = 100L * 1024 * 1024
    }
}
