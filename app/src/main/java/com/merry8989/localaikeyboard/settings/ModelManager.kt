package com.merry8989.localaikeyboard.settings

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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
 * Owns the on-device model files and downloads them.
 *
 * Downloads are done **in-app** (not via the system DownloadManager) so the user
 * sees real progress and real error messages instead of a silent failure. Files
 * live in app-private storage, so no storage permission is needed.
 */
class ModelManager(private val context: Context) {

    val modelsDir: File = File(context.filesDir, "models").apply { mkdirs() }

    fun fileFor(spec: ModelSpec): File = File(modelsDir, spec.fileName)

    fun isInstalled(spec: ModelSpec): Boolean =
        fileFor(spec).let { it.exists() && it.length() > MIN_BYTES }

    /** The first available model that is actually installed, if any. */
    fun installedSpec(): ModelSpec? = AVAILABLE.firstOrNull { isInstalled(it) }

    fun modelPath(spec: ModelSpec?): String? =
        spec?.takeIf { isInstalled(it) }?.let { fileFor(it).absolutePath }

    fun activeModelPath(): String? = modelPath(installedSpec())

    fun delete(spec: ModelSpec): Boolean {
        File(modelsDir, spec.fileName + ".part").delete()
        return fileFor(spec).delete()
    }

    /** Remove any leftover partial downloads. */
    fun clearPartials() {
        modelsDir.listFiles()?.filter { it.name.endsWith(".part") }?.forEach { it.delete() }
    }

    /**
     * Download [spec] to app-private storage, reporting 0..100 progress.
     * [onProgress] is invoked on a background thread.
     */
    suspend fun download(spec: ModelSpec, onProgress: (Int) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            val dest = fileFor(spec)
            val part = File(modelsDir, spec.fileName + ".part")
            try {
                part.delete()
                val conn = (URL(spec.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "LocalAIKeyboard/1.0")
                }
                conn.connect()
                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    return@withContext Result.failure(
                        Exception("Server returned HTTP $code. The link may have moved or need sign-in.")
                    )
                }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        var lastPct = -1
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = ((done * 100) / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
                conn.disconnect()

                dest.delete()
                if (!part.renameTo(dest)) {
                    part.copyTo(dest, overwrite = true)
                    part.delete()
                }
                if (isInstalled(spec)) Result.success(dest)
                else Result.failure(Exception("Downloaded file looks incomplete (${dest.length()} bytes)."))
            } catch (t: Throwable) {
                part.delete()
                Result.failure(t)
            }
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
