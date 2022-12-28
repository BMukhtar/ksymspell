package io.gitlab.rxp90.jsymspell.api

/**
 * Holds a pair of words.
 */
data class Bigram(private val word1: String, private val word2: String) {
    override fun toString(): String {
        return "$word1 $word2"
    }
}