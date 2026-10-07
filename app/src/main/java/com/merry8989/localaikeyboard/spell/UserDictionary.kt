package com.merry8989.localaikeyboard.spell

import android.content.Context
import java.io.File

/**
 * The user's own dictionary: words they type that aren't in the built-in list.
 * Stored as a small text file in app-private storage — entirely on the device,
 * no network, no accounts.
 */
class UserDictionary(private val context: Context) {

    private val file = File(context.filesDir, "user_dictionary.txt")
    private val words = LinkedHashMap<String, Int>()

    init {
        load()
    }

    /** Learn a word (or reinforce one already known). */
    @Synchronized
    fun add(word: String, weight: Int = 1) {
        val w = normalize(word)
        if (w.length < 2) return
        if (!w.all { it.isLetter() || it == '\'' || it == '-' }) return
        words[w] = (words[w] ?: 0) + weight
        save()
    }

    @Synchronized
    fun contains(word: String): Boolean = words.containsKey(normalize(word))

    @Synchronized
    fun all(): List<Pair<String, Int>> =
        words.entries.sortedByDescending { it.value }.map { it.key to it.value }

    @Synchronized
    fun remove(word: String) {
        if (words.remove(normalize(word)) != null) save()
    }

    @Synchronized
    fun clear() {
        words.clear()
        save()
    }

    @Synchronized
    fun count(): Int = words.size

    private fun normalize(word: String) = word.trim().lowercase()

    private fun load() {
        if (!file.exists()) return
        runCatching {
            file.forEachLine { line ->
                val parts = line.split('\t')
                if (parts.size == 2) {
                    val c = parts[1].toIntOrNull() ?: 1
                    if (parts[0].isNotBlank()) words[parts[0]] = c
                }
            }
        }
    }

    @Synchronized
    private fun save() {
        runCatching {
            file.writeText(words.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        }
    }
}
