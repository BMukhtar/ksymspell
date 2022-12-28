package io.gitlab.rxp90.jsymspell.api

class DamerauLevenshteinOSA : StringDistance {
    private val charComparator: CharComparator

    constructor() {
        charComparator = DefaultCharComparator()
    }

    constructor(charComparator: CharComparator) {
        this.charComparator = charComparator
    }

    override fun distanceWithEarlyStop(baseString: String, string2: String, maxDistance: Int): Int {
        var string2 = string2
        var maxDistance = maxDistance
        if (baseString == null) return string2?.length ?: 0
        if (string2 == null || string2.isEmpty()) return baseString.length
        if (maxDistance == 0) return if (baseString == string2) 0 else -1
        var baseChar1Costs = IntArray(baseString.length)
        var basePrevChar1Costs = IntArray(baseString.length)

        // If strings have different lengths, ensure shorter string is in string1. This can result in a
        // little faster speed by spending more time spinning just the inner loop during the main processing.
        val string1: String
        if (baseString.length > string2.length) {
            string1 = string2
            string2 = baseString
        } else {
            string1 = baseString
        }
        var str1Len = string1.length
        var str2Len = string2.length

        // Ignore common suffix
        while (str1Len > 0 && string1[str1Len - 1] == string2[str2Len - 1]) {
            str1Len--
            str2Len--
        }
        var start = 0
        if (string1[0] == string2[0] || str1Len == 0) {
            // Ignore common prefix and string1 substring of string2
            while (start < str1Len && string1[start] == string2[start]) start++
            str1Len -= start // length of the part excluding common prefix and suffix
            str2Len -= start
            if (str1Len == 0) { // string1 is a substring in string2, so str2Len == distance between both
                return str2Len
            }
            string2 = string2.substring(start, start + str2Len) // faster than string2[start+j] in inner loop below
        }
        val lenDiff = str2Len - str1Len
        if (maxDistance < 0 || maxDistance > str2Len) {
            maxDistance = str2Len
        } else if (lenDiff > maxDistance) {
            return -1
        }
        if (str2Len > baseChar1Costs.size) {
            baseChar1Costs = IntArray(str2Len)
            basePrevChar1Costs = IntArray(str2Len)
        } else {
            for (i in 0 until str2Len) {
                basePrevChar1Costs[i] = 0
            }
        }
        for (j in 0 until str2Len) {
            if (j < maxDistance) {
                baseChar1Costs[j] = j + 1
            } else {
                baseChar1Costs[j] = maxDistance + 1
            }
        }
        val jStartOffset = maxDistance - (str2Len - str1Len)
        val haveMax = maxDistance < str2Len
        var jStart = 0
        var jEnd = maxDistance
        var str1Char = string1[0]
        var current = 0
        for (i in 0 until str1Len) {
            val prevStr1Char = str1Char
            str1Char = string1[start + i]
            var str2Char = string2[0]
            var left = i
            current = left + 1
            var nextTransCost = 0
            // no need to look beyond window of lower right diagonal - maxDistance cells (lower right diag is i - lenDiff) and the upper left diagonal + maxDistance cells (upper left is i)
            jStart += if (i > jStartOffset) 1 else 0
            jEnd += if (jEnd < str2Len) 1 else 0
            for (j in jStart until jEnd) {
                val above = current
                var thisTransCost = nextTransCost
                nextTransCost = basePrevChar1Costs[j]
                current = left
                basePrevChar1Costs[j] = current // cost of diagonal (substitution)
                left = baseChar1Costs[j] // left now equals current cost (which will be diagonal at next iteration)
                val prevStr2Char = str2Char
                str2Char = string2[j]
                if (charComparator.areDistinct(str1Char, str2Char)) {
                    if (left < current) current = left // insertion
                    if (above < current) current = above // deletion
                    current++
                    if (i != 0 && j != 0
                        && charComparator.areEqual(str1Char, prevStr2Char)
                        && charComparator.areEqual(prevStr1Char, str2Char)
                    ) {
                        thisTransCost++
                        if (thisTransCost < current) current = thisTransCost // transposition
                    }
                }
                baseChar1Costs[j] = current
            }
            if (haveMax && baseChar1Costs[i + lenDiff] > maxDistance) return -1
        }
        return if (current <= maxDistance) current else -1
    }
}