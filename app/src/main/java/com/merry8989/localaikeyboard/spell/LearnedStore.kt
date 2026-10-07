package com.merry8989.localaikeyboard.spell

import android.content.Context
import java.io.File

/**
 * Learns what you actually type: word frequencies and word-to-word pairs, so
 * the keyboard can predict the next word. Stored in app-private storage on the
 * device only.
 */
class LearnedStore(@Suppress("unused") private val context: Context) {

    private val file = File(context.filesDir, "learned.txt")
    private val unigrams = HashMap<String, Int>()
    private val bigrams = HashMap<String, HashMap<String, Int>>()   // prev -> next -> count
    private var lastSaveAt = 0L

    init {
        load()
    }

    /** Record that [word] was typed after [prev]. */
    @Synchronized
    fun observeWord(prev: String?, word: String) {
        val w = norm(word)
        if (w.length < 2 || !w.all { it.isLetter() || it == '\'' || it == '-' }) return
        unigrams[w] = (unigrams[w] ?: 0) + 1

        val p = prev?.let { norm(it) }
        if (!p.isNullOrBlank()) {
            val m = bigrams.getOrPut(p) { HashMap() }
            m[w] = (m[w] ?: 0) + 1
        }
        maybeSave()
    }

    /** Words most likely to follow [prev]. */
    @Synchronized
    fun nextWords(prev: String, limit: Int = 3): List<String> {
        val m = bigrams[norm(prev)] ?: return emptyList()
        return m.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    @Synchronized
    fun topWords(limit: Int): List<String> =
        unigrams.entries.sortedByDescending { it.value }.take(limit).map { it.key }

    @Synchronized
    fun wordCount(): Int = unigrams.size

    @Synchronized
    fun pairCount(): Int = bigrams.size

    @Synchronized
    fun clear() {
        unigrams.clear()
        bigrams.clear()
        save()
    }

    private fun norm(w: String) = w.trim().lowercase()

    private fun maybeSave() {
        val now = System.currentTimeMillis()
        if (now - lastSaveAt > 5_000) save()
    }

    @Synchronized
    private fun save() {
        lastSaveAt = System.currentTimeMillis()
        runCatching {
            file.bufferedWriter().use { w ->
                for ((word, count) in unigrams) w.write("u\t$word\t$count\n")
                for ((prev, nexts) in bigrams) {
                    for ((next, count) in nexts) w.write("b\t$prev\t$next\t$count\n")
                }
            }
        }
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            file.forEachLine { line ->
                val p = line.split('\t')
                when {
                    p.size == 3 && p[0] == "u" -> unigrams[p[1]] = p[2].toIntOrNull() ?: 1
                    p.size == 4 && p[0] == "b" -> {
                        val m = bigrams.getOrPut(p[1]) { HashMap() }
                        m[p[2]] = p[3].toIntOrNull() ?: 1
                    }
                }
            }
        }
    }
}
