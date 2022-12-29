package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.Bigram
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import java.util.stream.Collectors


fun main() {

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

    val symSpell: SymSpell = SymSpellBuilder().setMaxDictionaryEditDistance(2)
        .setUnigramLexicon(unigrams)
        .createSymSpell()
    val suggestions = symSpell.lookup("sumarized", Verbosity.ALL)
    println(suggestions[0])
}