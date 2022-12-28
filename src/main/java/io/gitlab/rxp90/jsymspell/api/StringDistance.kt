package io.gitlab.rxp90.jsymspell.api

interface StringDistance {
    /**
     * Calculates the distance between `string1` and `string2`, early stopping at `maxDistance`.
     * @param string1 first string
     * @param string2 second string
     * @param maxDistance distance at which the algorithm will stop early
     * @return distance between `string1` and `string2`, early stopping at `maxDistance`, or `-1` if `maxDistance` was reached
     */
    fun distanceWithEarlyStop(string1: String, string2: String, maxDistance: Int): Int

    /**
     * @see StringDistance.distanceWithEarlyStop
     * @param string1 first string
     * @param string2 second string
     * @return distance between `string1` and `string2`
     */
    fun distance(string1: String, string2: String): Int {
        return distanceWithEarlyStop(string1, string2, Math.max(string1.length, string2.length))
    }
}