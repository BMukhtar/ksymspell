package io.gitlab.rxp90.jsymspell

/**
 * Allows to control the number of returned results
 */
enum class Verbosity {
    /**
     * Top suggestion with the highest term frequency of the suggestions of smallest edit distance found
     */
    TOP,

    /**
     * All suggestions of smallest edit distance found
     */
    CLOSEST,

    /**
     * All suggestions within [SymSpell.getMaxDictionaryEditDistance]
     */
    ALL
}