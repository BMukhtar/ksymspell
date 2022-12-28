package io.gitlab.rxp90.jsymspell

import io.gitlab.rxp90.jsymspell.api.SuggestItem
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class SuggestItemTest {
    @Test
    fun compareToAscendingByDistance() {
        val smallDistance = SuggestItem("test", 5, 10.0)
        val bigDistance = SuggestItem("test", 10, 10.0)
        Assertions.assertEquals(-1, smallDistance.compareTo(bigDistance))
    }

    @Test
    fun compareToDescendingByFrequency() {
        val sameDistanceSmallerFreq = SuggestItem("test", 5, 10.0)
        val sameDistanceBiggerFreq = SuggestItem("test", 5, 20.0)
        Assertions.assertEquals(1, sameDistanceSmallerFreq.compareTo(sameDistanceBiggerFreq))
    }

    @Test
    fun equalsAndHashCode() {
        val si1 = SuggestItem("test", 5, 10.0)
        val si2 = SuggestItem("test", 5, 10.0)
        Assertions.assertEquals(si1, si2)
        Assertions.assertEquals(si1.hashCode(), si2.hashCode())
    }
}