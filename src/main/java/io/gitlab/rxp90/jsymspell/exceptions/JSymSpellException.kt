package io.gitlab.rxp90.jsymspell.exceptions

open class JSymSpellException : Exception {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}