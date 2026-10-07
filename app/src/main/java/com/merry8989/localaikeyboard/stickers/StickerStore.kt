package com.merry8989.localaikeyboard.stickers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * A local library of stickers / GIFs / photos. Files the user imports are copied
 * into app-private storage so they live on the device and need no network.
 *
 * NOTE: an Android keyboard cannot insert an image into a normal text field —
 * InputConnection only carries text. So "using" a sticker means copying it to
 * the clipboard (to paste into a chat app) or sharing it. That is what
 * [copyToClipboard] and [shareIntent] do.
 */
class StickerStore(private val context: Context) {

    val dir: File = File(context.filesDir, "stickers").apply { mkdirs() }

    fun list(): List<File> =
        dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun count(): Int = list().size

    /** Copy an imported content Uri into local storage. Returns the new file. */
    fun import(uri: Uri): File? {
        val base = uri.lastPathSegment?.substringAfterLast('/')?.takeLast(24) ?: "media"
        val safe = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dest = File(dir, "sticker_${System.currentTimeMillis()}_$safe")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            if (dest.length() > 0) dest else null
        } catch (e: Exception) {
            dest.delete()
            null
        }
    }

    fun delete(file: File): Boolean = file.delete()

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Copy the sticker to the clipboard as an image Uri, so the user can paste
     * it into a chat app.
     */
    fun copyToClipboard(file: File) {
        val uri = uriFor(file)
        val clip = ClipData.newUri(context.contentResolver, "sticker", uri)
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(clip)
    }

    fun shareIntent(file: File): Intent {
        val uri = uriFor(file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
