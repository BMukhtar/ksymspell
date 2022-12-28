package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.Bigram
import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA
import io.gitlab.rxp90.jsymspell.api.StringDistance

class SymSpellBuilder {
    var maxDictionaryEditDistance = 2
        private set
    var prefixLength = 7
        private set
    var stringDistanceAlgorithm: StringDistance = DamerauLevenshteinOSA()
        private set
    var unigramLexicon: Map<String, Long> = HashMap()
        private set
    var bigramLexicon: Map<Bigram, Long> = HashMap()
        private set

    fun setMaxDictionaryEditDistance(maxDictionaryEditDistance: Int): SymSpellBuilder {
        this.maxDictionaryEditDistance = maxDictionaryEditDistance
        return this
    }

    fun setPrefixLength(prefixLength: Int): SymSpellBuilder {
        this.prefixLength = prefixLength
        return this
    }

    fun setUnigramLexicon(unigramLexicon: Map<String, Long>): SymSpellBuilder {
        this.unigramLexicon = unigramLexicon
        return this
    }

    fun setBigramLexicon(bigramLexicon: Map<Bigram, Long>): SymSpellBuilder {
        this.bigramLexicon = bigramLexicon
        return this
    }

    fun setStringDistanceAlgorithm(distanceAlgorithm: StringDistance): SymSpellBuilder {
        stringDistanceAlgorithm = distanceAlgorithm
        return this
    }

    fun createSymSpell(): SymSpellImpl {
        return SymSpellImpl(this)
    }
}