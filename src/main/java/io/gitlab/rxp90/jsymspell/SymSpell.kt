package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.Bigram
import io.gitlab.rxp90.jsymspell.api.SuggestItem
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException

/**
 * Port of SymSpell: 1 million times faster spelling correction and fuzzy search through Symmetric Delete spelling correction algorithm
 *
 * @see [https://github.com/wolfgarbe/SymSpell](https://github.com/wolfgarbe/SymSpell)
 */
interface SymSpell {
    /**
     * Returns a sorted `List` of `SuggestItem` for a given `input`
     * @param input string to apply spelling correction to
     * @param verbosity see [Verbosity]
     * @param includeUnknown controls whether non-lexicon words should be considered
     * @return sorted `List` of `SuggestItem` for a given `input`
     * @throws NotInitializedException if no unigram lexicon has been provided, i.e. [SymSpell.getUnigramLexicon] is empty
     */
    @Throws(NotInitializedException::class)
    fun lookup(input: String?, verbosity: Verbosity?, includeUnknown: Boolean): List<SuggestItem?>?

    /**
     * Same as [SymSpell.lookup] where `includeUnknown` is false
     * @see SymSpell.lookup
     * @param input string to apply spelling correction to
     * @param verbosity see [Verbosity]
     * @return sorted `List` of `SuggestItem` for a given `input`
     * @throws NotInitializedException if no unigram lexicon has been provided, i.e. [SymSpell.getUnigramLexicon] is empty
     */
    @Throws(NotInitializedException::class)
    fun lookup(input: String?, verbosity: Verbosity?): List<SuggestItem?>?

    /**
     * Performs spelling correction of multiple space separated words.
     * @param input string to apply spelling correction to, where words are separated by spaces
     * @param editDistanceMax limit up to which lexicon words can be considered suggestions, must be lower or equal than [SymSpell.getMaxDictionaryEditDistance]
     * @param includeUnknown controls whether non-lexicon words should be considered
     * @return sorted `List` of `SuggestItem` for a given `input`
     * @throws NotInitializedException if no unigram, and/or bigram lexicon has been provided, i.e. [SymSpell.getUnigramLexicon] is empty, and/or [SymSpell.getBigramLexicon] is empty
     */
    @Throws(NotInitializedException::class)
    fun lookupCompound(input: String?, editDistanceMax: Int, includeUnknown: Boolean): List<SuggestItem?>?

    /**
     * Map where the key is a word of the lexicon and the value is the frequency.
     * @return map where the key is a word of the lexicon and the value is the frequency
     */
    val unigramLexicon: Map<String?, Long?>?

    /**
     * Map where the key is a pair of words of the lexicon and the value is the frequency.
     * @see Bigram
     *
     * @return map where the key is a pair of words of the lexicon and the value is the frequency
     */
    val bigramLexicon: Map<Bigram?, Long?>?

    /**
     * Limit up to which lexicon words can be considered suggestions.
     * @return limit up to which lexicon words can be considered suggestions
     */
    val maxDictionaryEditDistance: Int
}