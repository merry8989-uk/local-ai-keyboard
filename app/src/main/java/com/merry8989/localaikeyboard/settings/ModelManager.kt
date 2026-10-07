package com.merry8989.localaikeyboard.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File

/** One downloadable on-device model. */
data class ModelSpec(
    val id: String,
    val name: String,
    val url: String,
    val fileName: String,
    val approxBytes: Long,
) {
    val approxLabel: String get() = "%.2f GB".format(approxBytes / 1024.0 / 1024.0 / 1024.0)
}

/**
 * Owns the on-device model files: which are present, how to fetch them, how to
 * delete them. Downloads go through the system DownloadManager (a separate
 * process), which is why the keyboard itself needs no INTERNET permission.
 *
 * Model URLs below were verified reachable and ungated. They are Hugging Face
 * "resolve" URLs; the file format is the `.task` bundle the MediaPipe LLM
 * Inference runtime reads.
 */
class ModelManager(private val context: Context) {

    val modelsDir: File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "models").apply { mkdirs() }

    fun fileFor(spec: ModelSpec): File = File(modelsDir, spec.fileName)

    fun isInstalled(spec: ModelSpec): Boolean =
        fileFor(spec).let { it.exists() && it.length() > MIN_BYTES }

    /** The first available model that is actually installed, if any. */
    fun installedSpec(): ModelSpec? = AVAILABLE.firstOrNull { isInstalled(it) }

    fun modelPath(spec: ModelSpec?): String? =
        spec?.takeIf { isInstalled(it) }?.let { fileFor(it).absolutePath }

    fun activeModelPath(): String? = modelPath(installedSpec())

    fun delete(spec: ModelSpec): Boolean = fileFor(spec).delete()

    /**
     * Start a model download. Returns the DownloadManager request id, or -1 if
     * the download manager is unavailable.
     */
    fun enqueueDownload(spec: ModelSpec): Long {
        val request = DownloadManager.Request(Uri.parse(spec.url))
            .setTitle("Local AI Keyboard model")
            .setDescription(spec.fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "models/${spec.fileName}")
        val dm = context.getSystemService(DownloadManager::class.java) ?: return -1L
        return dm.enqueue(request)
    }

    companion object {
        private const val HF = "https://huggingface.co/litert-community"

        /** Default: good quality/size balance, ungated, `.task` format. */
        val QWEN_15B = ModelSpec(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen2.5 1.5B Instruct",
            url = "$HF/Qwen2.5-1.5B-Instruct/resolve/main/" +
                "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen2.5-1.5b-instruct-q8.task",
            approxBytes = 1_600_000_000L,
        )

        /** Fastest option, for low-end or battery-conscious use. */
        val QWEN_05B = ModelSpec(
            id = "qwen2.5-0.5b-instruct",
            name = "Qwen2.5 0.5B Instruct (fastest)",
            url = "$HF/Qwen2.5-0.5B-Instruct/resolve/main/" +
                "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen2.5-0.5b-instruct-q8.task",
            approxBytes = 550_000_000L,
        )

        val AVAILABLE = listOf(QWEN_15B, QWEN_05B)
        val DEFAULT = QWEN_15B

        /** Anything smaller than this is a failed/partial download. */
        private const val MIN_BYTES = 50L * 1024 * 1024
    }
}
