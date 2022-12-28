package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.Bigram
import io.gitlab.rxp90.jsymspell.api.StringDistance
import io.gitlab.rxp90.jsymspell.api.SuggestItem
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.logging.Logger

class SymSpellImpl internal constructor(builder: SymSpellBuilder) : SymSpell {
    private val maxDictionaryEditDistance: Int
    private val prefixLength: Int

    /**
     * Map of Delete -> Collection of words that lead to that edited word
     */
    private val deletes: MutableMap<String, MutableCollection<String>> = ConcurrentHashMap()
    private val bigramLexicon: Map<Bigram, Long>
    private val unigramLexicon: Map<String, Long>
    private val stringDistance: StringDistance
    private val maxDictionaryWordLength: Int

    /**
     * Sum of all counts in the dictionary
     */
    private val n: Long

    init {
        unigramLexicon = HashMap(builder.unigramLexicon)
        maxDictionaryEditDistance = builder.maxDictionaryEditDistance
        prefixLength = builder.prefixLength
        bigramLexicon = HashMap(builder.bigramLexicon)
        stringDistance = builder.stringDistanceAlgorithm
        n = unigramLexicon.values.stream().reduce { a: Long, b: Long -> java.lang.Long.sum(a, b) }.orElse(0L)
        unigramLexicon.keys.forEach(Consumer { word: String ->
            val edits = generateEdits(word)
            edits.forEach { (string: String, suggestions: Collection<String>?) ->
                deletes.computeIfAbsent(string) { ignored: String? -> ArrayList() }
                    .addAll(
                        suggestions
                    )
            }
        })
        maxDictionaryWordLength = unigramLexicon.keys.stream().map { obj: String -> obj.length }
            .max { obj: Int, anotherInteger: Int? -> obj.compareTo(anotherInteger!!) }.orElse(0)
    }

    private fun deleteSuggestionPrefix(
        delete: String,
        deleteLen: Int,
        suggestion: String,
        suggestionLen: Int
    ): Boolean {
        if (deleteLen == 0) return true
        val adjustedSuggestionLen = Math.min(prefixLength, suggestionLen)
        var j = 0
        for (i in 0 until deleteLen) {
            val delChar = delete[i]
            while (j < adjustedSuggestionLen && delChar != suggestion[j]) {
                j++
            }
            if (j == adjustedSuggestionLen) return false
        }
        return true
    }

    fun edits(word: String, editDistance: Int, deleteWords: MutableSet<String>): Set<String> {
        var editDistance = editDistance
        editDistance++
        if (word.length > 1 && editDistance <= maxDictionaryEditDistance) {
            for (i in 0 until word.length) {
                val editableWord = StringBuilder(word)
                val delete = editableWord.deleteCharAt(i).toString()
                if (deleteWords.add(delete) && editDistance < maxDictionaryEditDistance) {
                    edits(delete, editDistance, deleteWords)
                }
            }
        }
        return deleteWords
    }

    private fun generateEdits(key: String): Map<String, MutableCollection<String>> {
        val edits = editsPrefix(key)
        val generatedDeletes: MutableMap<String, MutableCollection<String>> = HashMap()
        edits.forEach(Consumer { delete: String ->
            generatedDeletes.computeIfAbsent(delete) { ignored: String? -> ArrayList() }
                .add(key)
        })
        return generatedDeletes
    }

    private fun editsPrefix(key: String): Set<String> {
        var key = key
        val set: MutableSet<String> = HashSet()
        if (key.length <= maxDictionaryEditDistance) {
            set.add("")
        }
        if (key.length > prefixLength) {
            key = key.substring(0, prefixLength)
        }
        set.add(key)
        return edits(key, 0, set)
    }

    @Throws(NotInitializedException::class)
    override fun lookup(input: String, verbosity: Verbosity, includeUnknown: Boolean): List<SuggestItem> {
        return lookup(input, verbosity, maxDictionaryEditDistance, includeUnknown)
    }

    @Throws(NotInitializedException::class)
    override fun lookup(input: String, verbosity: Verbosity): List<SuggestItem> {
        return lookup(input, verbosity, false)
    }

    @Throws(NotInitializedException::class)
    private fun lookup(
        input: String,
        verbosity: Verbosity,
        maxEditDistance: Int,
        includeUnknown: Boolean
    ): List<SuggestItem> {
        require(maxEditDistance <= maxDictionaryEditDistance) { "maxEditDistance > maxDictionaryEditDistance" }
        if (unigramLexicon.isEmpty()) {
            throw NotInitializedException("There are no words in the lexicon.")
        }
        val suggestions: MutableList<SuggestItem> = ArrayList()
        val inputLen = input.length
        val wordIsTooLong = inputLen - maxEditDistance > maxDictionaryWordLength
        if (wordIsTooLong && includeUnknown) {
            return Arrays.asList(SuggestItem(input, maxEditDistance + 1, 0.0))
        }
        if (unigramLexicon.containsKey(input)) {
            val suggestSameWord = SuggestItem(input, 0, unigramLexicon[input]!!.toDouble())
            suggestions.add(suggestSameWord)
            if (verbosity != Verbosity.ALL) {
                return suggestions
            }
        }
        if (maxEditDistance == 0 && includeUnknown && suggestions.isEmpty()) {
            return Arrays.asList(SuggestItem(input, maxEditDistance + 1, 0.0))
        }
        val deletesAlreadyConsidered: MutableSet<String> = HashSet()
        val candidates: MutableList<String> = ArrayList()
        val inputPrefixLen: Int
        if (inputLen > prefixLength) {
            inputPrefixLen = prefixLength
            candidates.add(input.substring(0, inputPrefixLen))
        } else {
            inputPrefixLen = inputLen
        }
        candidates.add(input)
        val suggestionsAlreadyConsidered: MutableSet<String> = HashSet()
        suggestionsAlreadyConsidered.add(input)
        var maxEditDistance2 = maxEditDistance
        var candidatePointer = 0
        while (candidatePointer < candidates.size) {
            val candidate = candidates[candidatePointer++]
            val candidateLength = candidate.length
            val lengthDiffBetweenInputAndCandidate = inputPrefixLen - candidateLength
            val candidateDistanceHigherThanSuggestionDistance = lengthDiffBetweenInputAndCandidate > maxEditDistance2
            if (candidateDistanceHigherThanSuggestionDistance) {
                if (verbosity == Verbosity.ALL) {
                    continue
                } else {
                    break
                }
            }
            if (lengthDiffBetweenInputAndCandidate < maxEditDistance && candidateLength <= prefixLength) {
                if (verbosity != Verbosity.ALL && lengthDiffBetweenInputAndCandidate >= maxEditDistance2) {
                    continue
                }
                candidates.addAll(generateNewCandidates(candidate, deletesAlreadyConsidered))
            }
            val preCalculatedDeletes: Collection<String>? = deletes[candidate]
            if (preCalculatedDeletes != null) {
                for (preCalculatedDelete in preCalculatedDeletes) {
                    if ((preCalculatedDelete == input || Math.abs(preCalculatedDelete.length - inputLen) > maxEditDistance2 || preCalculatedDelete.length < candidateLength || preCalculatedDelete.length == candidateLength) && preCalculatedDelete != candidate || (Math.min(
                            preCalculatedDelete.length,
                            prefixLength
                        ) > inputPrefixLen
                                && Math.min(
                            preCalculatedDelete.length,
                            prefixLength
                        ) - candidateLength > maxEditDistance2)
                    ) {
                        continue
                    }
                    var distance: Int
                    if (candidateLength == 0) {
                        distance = Math.max(inputLen, preCalculatedDelete.length)
                        if (distance <= maxEditDistance2) {
                            suggestionsAlreadyConsidered.add(preCalculatedDelete)
                        }
                    } else if (preCalculatedDelete.length == 1) {
                        distance = if (input.contains(preCalculatedDelete)) {
                            inputLen - 1
                        } else {
                            inputLen
                        }
                        if (distance <= maxEditDistance2) {
                            suggestionsAlreadyConsidered.add(preCalculatedDelete)
                        }
                    } else {
                        val minDistance = Math.min(inputLen, preCalculatedDelete.length) - prefixLength
//                      /*
//                      boolean noDistanceCalculationIsRequired = prefixLength - maxEditDistance == candidateLength
//                                && (minDistance > 1 && (!input.substring(inputLen + 1 - minDistance).equals(preCalculatedDelete.substring(preCalculatedDelete.length() + 1 - minDistance))))
//                                || (minDistance > 0
//                                    && input.charAt(inputLen - minDistance) != preCalculatedDelete.charAt(preCalculatedDelete.length() - minDistance)
//                                    && input.charAt(inputLen - minDistance - 1) != preCalculatedDelete.charAt(preCalculatedDelete.length() - minDistance)
//                                    && input.charAt(inputLen - minDistance) != preCalculatedDelete.charAt(preCalculatedDelete.length() - minDistance - 1));
//                      */
                        val noDistanceCalculationIsRequired =
                            prefixLength - maxEditDistance == candidateLength
                                    && (minDistance > 1 && input.substring(inputLen + 1 - minDistance)!= preCalculatedDelete.substring(preCalculatedDelete.length + 1 - minDistance))
                                    || (minDistance > 0
                                    && input[inputLen - minDistance] != preCalculatedDelete[preCalculatedDelete.length - minDistance]
                                    && input[inputLen - minDistance - 1] != preCalculatedDelete[preCalculatedDelete.length - minDistance]
                                    && input[inputLen - minDistance] != preCalculatedDelete[preCalculatedDelete.length - minDistance - 1])
                        if (noDistanceCalculationIsRequired) {
                            continue
                        } else {
                            if (verbosity != Verbosity.ALL
                                && !deleteSuggestionPrefix(
                                    candidate,
                                    candidateLength,
                                    preCalculatedDelete,
                                    preCalculatedDelete.length
                                )
                                || !suggestionsAlreadyConsidered.add(preCalculatedDelete)
                            ) {
                                continue
                            }
                            distance =
                                stringDistance.distanceWithEarlyStop(input, preCalculatedDelete, maxEditDistance2)
                            if (distance < 0) {
                                continue
                            }
                        }
                        if (distance <= maxEditDistance2) {
                            val suggestItem = SuggestItem(
                                preCalculatedDelete, distance, unigramLexicon[preCalculatedDelete]!!
                                    .toDouble()
                            )
                            if (!suggestions.isEmpty()) {
                                if (verbosity == Verbosity.CLOSEST && distance < maxEditDistance2) {
                                    suggestions.clear()
                                } else if (verbosity == Verbosity.TOP && (distance < maxEditDistance2 || suggestItem.frequencyOfSuggestionInDict > suggestions[0].frequencyOfSuggestionInDict)) {
                                    maxEditDistance2 = distance
                                    suggestions.add(suggestItem)
                                }
                            }
                            if (verbosity != Verbosity.ALL) {
                                maxEditDistance2 = distance
                            }
                            suggestions.add(suggestItem)
                        }
                    }
                }
            }
        }
        if (suggestions.size > 1) {
            Collections.sort(suggestions)
        }
        if (includeUnknown && suggestions.isEmpty()) {
            val noSuggestionsFound = SuggestItem(input, maxEditDistance + 1, 0.0)
            suggestions.add(noSuggestionsFound)
        }
        return suggestions
    }

    private fun generateNewCandidates(candidate: String, deletesAlreadyConsidered: MutableSet<String>): Set<String> {
        val newDeletes: MutableSet<String> = HashSet()
        for (i in 0 until candidate.length) {
            val editableString = StringBuilder(candidate)
            val delete = editableString.deleteCharAt(i).toString()
            if (deletesAlreadyConsidered.add(delete)) {
                newDeletes.add(delete)
            }
        }
        return newDeletes
    }

    @Throws(NotInitializedException::class)
    override fun lookupCompound(input: String, editDistanceMax: Int, includeUnknown: Boolean): List<SuggestItem> {
        val termList = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val suggestionParts: MutableList<SuggestItem> = ArrayList()
        var lastCombination = false
        for (i in termList.indices) {
            val currentToken = termList[i]
            val suggestionsForCurrentToken = lookup(currentToken, Verbosity.TOP, editDistanceMax, includeUnknown)
            if (i > 0 && !lastCombination) {
                val bestSuggestion = suggestionParts[suggestionParts.size - 1]
                val newSuggestion = combineWords(
                    editDistanceMax,
                    includeUnknown,
                    currentToken,
                    termList[i - 1],
                    bestSuggestion,
                    if (suggestionsForCurrentToken.isEmpty()) null else suggestionsForCurrentToken[0]
                )
                if (newSuggestion.isPresent) {
                    suggestionParts[suggestionParts.size - 1] = newSuggestion.get()
                    lastCombination = true
                    continue
                }
            }
            lastCombination = false
            if (!suggestionsForCurrentToken.isEmpty()) {
                val firstSuggestionIsPerfect = suggestionsForCurrentToken[0].editDistance == 0
                if (firstSuggestionIsPerfect || currentToken.length == 1) {
                    suggestionParts.add(suggestionsForCurrentToken[0])
                } else {
                    splitWords(editDistanceMax, termList, suggestionsForCurrentToken, suggestionParts, i)
                }
            } else {
                splitWords(editDistanceMax, termList, suggestionsForCurrentToken, suggestionParts, i)
            }
        }
        var freq = n.toDouble()
        val stringBuilder = StringBuilder()
        for (suggestItem in suggestionParts) {
            stringBuilder.append(suggestItem.suggestion).append(" ")
            freq *= suggestItem.frequencyOfSuggestionInDict / n
        }
        val term = stringBuilder.toString()
            .replaceFirst("\\s++$".toRegex(), "") // this replace call trims all trailing whitespace
        val suggestion = SuggestItem(term, stringDistance.distanceWithEarlyStop(input, term, Int.MAX_VALUE), freq)
        val suggestionsLine: MutableList<SuggestItem> = ArrayList()
        suggestionsLine.add(suggestion)
        return suggestionsLine
    }

    @Throws(NotInitializedException::class)
    private fun splitWords(
        editDistanceMax: Int,
        termList: Array<String>,
        suggestions: List<SuggestItem>,
        suggestionParts: MutableList<SuggestItem>,
        i: Int
    ) {
        var suggestionSplitBest: SuggestItem? = null
        if (!suggestions.isEmpty()) suggestionSplitBest = suggestions[0]
        val word = termList[i]
        if (word.length > 1) {
            for (j in 1 until word.length) {
                val part1 = word.substring(0, j)
                val part2 = word.substring(j)
                var suggestionSplit: SuggestItem
                val suggestions1 = lookup(part1, Verbosity.TOP, editDistanceMax, false)
                if (!suggestions1.isEmpty()) {
                    val suggestions2 = lookup(part2, Verbosity.TOP, editDistanceMax, false)
                    if (!suggestions2.isEmpty()) {
                        val splitTerm = Bigram(suggestions1[0].suggestion, suggestions2[0].suggestion)
                        var splitDistance =
                            stringDistance.distanceWithEarlyStop(word, splitTerm.toString(), editDistanceMax)
                        if (splitDistance < 0) splitDistance = editDistanceMax + 1
                        if (suggestionSplitBest != null) {
                            if (splitDistance > suggestionSplitBest.editDistance) continue
                            if (splitDistance < suggestionSplitBest.editDistance) suggestionSplitBest = null
                        }
                        var freq: Double
                        if (bigramLexicon.containsKey(splitTerm)) {
                            freq = bigramLexicon[splitTerm]!!.toDouble()
                            if (!suggestions.isEmpty()) {
                                if (suggestions1[0].suggestion + suggestions2[0].suggestion == word) {
                                    freq = Math.max(freq, suggestions[0].frequencyOfSuggestionInDict + 2)
                                } else if ((suggestions1[0]
                                        .suggestion
                                            == suggestions[0].suggestion) || (suggestions2[0]
                                        .suggestion
                                            == suggestions[0].suggestion)
                                ) {
                                    freq = Math.max(freq, suggestions[0].frequencyOfSuggestionInDict + 1)
                                }
                            } else if (suggestions1[0].suggestion + suggestions2[0].suggestion == word) {
                                freq = Math.max(
                                    freq,
                                    Math.max(
                                        suggestions1[0].frequencyOfSuggestionInDict,
                                        suggestions2[0].frequencyOfSuggestionInDict
                                    )
                                )
                            }
                        } else {
                            // The Naive Bayes probability of the word combination is the product of the two
                            // word probabilities: P(AB) = P(A) * P(B)
                            // use it to estimate the frequency count of the combination, which then is used
                            // to rank/select the best splitting variant
                            freq =
                                Math.min(BIGRAM_COUNT_MIN, getNaiveBayesProbOfCombination(suggestions1, suggestions2))
                                    .toDouble()
                        }
                        suggestionSplit = SuggestItem(splitTerm.toString(), splitDistance, freq)
                        if (suggestionSplitBest == null || suggestionSplit.frequencyOfSuggestionInDict > suggestionSplitBest.frequencyOfSuggestionInDict) {
                            suggestionSplitBest = suggestionSplit
                        }
                    }
                }
            }
            if (suggestionSplitBest != null) {
                suggestionParts.add(suggestionSplitBest)
            } else {
                val suggestItem = SuggestItem(
                    word,
                    editDistanceMax + 1,
                    estimatedWordOccurrenceProbability(word).toDouble()
                ) // estimated word occurrence probability P=10 / (N * 10^word length l)
                suggestionParts.add(suggestItem)
            }
        } else {
            val suggestItem =
                SuggestItem(word, editDistanceMax + 1, estimatedWordOccurrenceProbability(word).toDouble())
            suggestionParts.add(suggestItem)
        }
    }

    private fun getNaiveBayesProbOfCombination(suggestions1: List<SuggestItem>, suggestions2: List<SuggestItem>): Long {
        return (suggestions1[0].frequencyOfSuggestionInDict / n.toDouble() * suggestions2[0].frequencyOfSuggestionInDict).toLong()
    }

    private fun estimatedWordOccurrenceProbability(word: String): Long {
        return (10.0 / Math.pow(10.0, word.length.toDouble())).toLong()
    }

    @Throws(NotInitializedException::class)
    fun combineWords(
        editDistanceMax: Int,
        includeUnknown: Boolean,
        token: String,
        previousToken: String,
        suggestItem: SuggestItem,
        secondBestSuggestion: SuggestItem?
    ): Optional<SuggestItem> {
        val suggestionsCombination = lookup(previousToken + token, Verbosity.TOP, editDistanceMax, includeUnknown)
        if (!suggestionsCombination.isEmpty()) {
            val best2: SuggestItem
            // TODO fixme
            best2 = Optional.ofNullable(secondBestSuggestion).orElseGet {
                SuggestItem(
                    token,
                    editDistanceMax + 1,
                    estimatedWordOccurrenceProbability(token).toDouble()
                )
            }
            val distance = suggestItem.editDistance + best2.editDistance
            val firstSuggestion = suggestionsCombination[0]
            if (distance >= 0 && firstSuggestion.editDistance + 1 < distance
                || (firstSuggestion.editDistance + 1 == distance
                        && firstSuggestion.frequencyOfSuggestionInDict
                        > suggestItem.frequencyOfSuggestionInDict / n
                        * best2.frequencyOfSuggestionInDict)
            ) {
                return Optional.of(
                    SuggestItem(
                        firstSuggestion.suggestion,
                        firstSuggestion.editDistance,
                        firstSuggestion.frequencyOfSuggestionInDict
                    )
                )
            }
        }
        return Optional.empty()
    }

    override fun getUnigramLexicon(): Map<String, Long> {
        return unigramLexicon
    }

    override fun getBigramLexicon(): Map<Bigram, Long> {
        return bigramLexicon
    }

    fun getDeletes(): Map<String, MutableCollection<String>> {
        return deletes
    }

    override fun getMaxDictionaryEditDistance(): Int {
        return maxDictionaryEditDistance
    }

    companion object {
        private val logger = Logger.getLogger(SymSpellImpl::class.java.name)
        private const val BIGRAM_COUNT_MIN = Long.MAX_VALUE
    }
}