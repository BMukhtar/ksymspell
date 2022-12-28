package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.CharComparator
import io.gitlab.rxp90.jsymspell.api.DamerauLevenshteinOSA
import io.gitlab.rxp90.jsymspell.api.StringDistance
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DamerauLevenshteinOSATest {
    @Test
    fun distanceWithEarlyStop() {
        val distance = DAMERAU_LEVENSHTEIN_OSA.distanceWithEarlyStop("CA", "ABC", 3)
        Assertions.assertEquals(3, distance)
    }

    @Test
    fun distanceLargerThanMax() {
        val distance = DAMERAU_LEVENSHTEIN_OSA.distanceWithEarlyStop("abcdef", "ghijkl", 3)
        Assertions.assertEquals(-1, distance)
    }

    @Test
    fun maxDistance() {
        val distance = DAMERAU_LEVENSHTEIN_OSA.distance("abcdef", "ghijkl")
        Assertions.assertEquals(6, distance)
    }

    @Nested
    internal inner class CustomCharComparator {
        @Test
        fun similarChars() {
            val customCharComparator: CharComparator = object : CharComparator {
                override fun areEqual(ch1: Char, ch2: Char): Boolean {
                    return if (ch1 == 'ñ' || ch2 == 'ñ') {
                        ch1 == 'n' || ch2 == 'n'
                    } else ch1 == ch2
                }
            }
            val damerauLevenshteinOSA: StringDistance = DamerauLevenshteinOSA(customCharComparator)
            val distance = damerauLevenshteinOSA.distance("Espana", "España")
            Assertions.assertEquals(0, distance)
        }

        @Test
        fun ignoreCase() {
            val ignoreCaseCharComparator: CharComparator = object : CharComparator {
                override fun areEqual(ch1: Char, ch2: Char): Boolean {
                    return ch1.lowercaseChar() == ch2.lowercaseChar()
                }
            }
            val damerauLevenshteinOSA: StringDistance = DamerauLevenshteinOSA(ignoreCaseCharComparator)
            val distance = damerauLevenshteinOSA.distance("JSYMSPELL", "jsymspell")
            Assertions.assertEquals(0, distance)
        }
    }

    companion object {
        private val DAMERAU_LEVENSHTEIN_OSA = DamerauLevenshteinOSA()
    }
}