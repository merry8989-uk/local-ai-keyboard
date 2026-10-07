package com.merry8989.localaikeyboard.spell

import android.content.Context
import java.io.File

/**
 * Per-key custom long-press words, per language. Whatever you type here shows
 * up in that key's long-press popup. Stored in app-private storage on the
 * device only.
 *
 * Words are comma-separated on entry, so a single word cannot itself contain a
 * comma.
 */
class CustomWordsStore(private val context: Context) {

    private val file = File(context.filesDir, "custom_long_press.txt")
    private val map = HashMap<String, MutableList<String>>()   // "lang|key" -> words

    init {
        load()
    }

    @Synchronized
    fun words(lang: String, key: String): List<String> =
        map[id(lang, key)]?.toList() ?: emptyList()

    @Synchronized
    fun setWords(lang: String, key: String, words: List<String>) {
        val clean = words.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty()) map.remove(id(lang, key)) else map[id(lang, key)] = clean.toMutableList()
        save()
    }

    @Synchronized
    fun all(lang: String): Map<String, List<String>> =
        map.entries
            .filter { it.key.startsWith("$lang|") }
            .associate { it.key.substringAfter('|') to it.value.toList() }

    @Synchronized
    fun clear(lang: String) {
        map.keys.filter { it.startsWith("$lang|") }.forEach { map.remove(it) }
        save()
    }

    private fun id(lang: String, key: String) = "$lang|${key.lowercase()}"

    private fun save() {
        runCatching {
            file.bufferedWriter().use { w ->
                for ((k, v) in map) w.write("$k\t${v.joinToString(",")}\n")
            }
        }
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            file.forEachLine { line ->
                val parts = line.split('\t')
                if (parts.size == 2) {
                    val words = parts[1].split(',').filter { it.isNotEmpty() }.toMutableList()
                    if (words.isNotEmpty()) map[parts[0]] = words
                }
            }
        }
    }
}
