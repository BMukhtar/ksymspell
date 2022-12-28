package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.Bigram
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import java.util.stream.Collectors


fun main() {
    val bigrams = Files.lines(Paths.get("src/test/resources/bigrams.txt"))
        .map { line: String ->
            line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        }
        .collect(
            Collectors.toMap(
                Function { tokens: Array<String> ->
                    Bigram(
                        tokens[0],
                        tokens[1]
                    )
                },
                Function { tokens: Array<String> ->
                    tokens[2].toLong()
                })
        )
    val unigrams = Files.lines(Paths.get("src/test/resources/words.txt"))
        .map { line: String ->
            line.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        }
        .collect(
            Collectors.toMap(
                Function { tokens: Array<String> ->
                    tokens[0]
                },
                Function { tokens: Array<String> ->
                    tokens[1].toLong()
                })
        )

    val symSpell: SymSpell = SymSpellBuilder().setUnigramLexicon(unigrams)
        .setBigramLexicon(bigrams)
        .setMaxDictionaryEditDistance(2)
        .createSymSpell()

    val maxEditDistance = 2
    val includeUnknowns = false
    val suggestions = symSpell.lookupCompound(
        "Nostalgiais truly one of th greatests human weakneses",
        maxEditDistance,
        includeUnknowns
    )
    println(suggestions[0].suggestion)
}