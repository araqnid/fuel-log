package org.araqnid.fuellog.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.SelfDescribing
import com.natpryce.hamkrest.describe

fun <T> assumeThat(actual: T, matcher: Matcher<T>) {
    val result = matcher(actual)
    if (result is MatchResult.Mismatch)
        throw AssumptionViolatedException(assumption = null, matcher = matcher, result = result)
}

fun <T> assumeThat(message: String, actual: T, matcher: Matcher<T>) {
    val result = matcher(actual)
    if (result is MatchResult.Mismatch)
        throw AssumptionViolatedException(assumption = message, matcher = matcher, result = result)
}

class AssumptionViolatedException (private val assumption: String? = null,
                                   private val matcher: Matcher<*>,
                                   private val result: MatchResult.Mismatch)
    : org.junit.AssumptionViolatedException(assumption), SelfDescribing {

    override val message: String?
        get() = description

    override val description by lazy {
        buildString {
            if (assumption != null) {
                append(assumption)
                append(": ")
            }

            append("expected: ")
            append(describe(matcher))
            append(", but: ")
            append(describe(result))
        }
    }
}
