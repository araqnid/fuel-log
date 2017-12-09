package org.araqnid.fuellog.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.SelfDescribing
import com.natpryce.hamkrest.describe

fun <T> assumeThat(actual: T, matcher: Matcher<T>) {
    val result = matcher(actual)
    if (result is MatchResult.Mismatch)
        throw AssumptionViolatedException.create(actual, matcher)
}

fun <T> assumeThat(message: String, actual: T, matcher: Matcher<T>) {
    val result = matcher(actual)
    if (result is MatchResult.Mismatch)
        throw AssumptionViolatedException.create(message, actual, matcher)
}

class AssumptionViolatedException private constructor(private val assumption: String? = null, private val actual: Any?, private val matcher: Matcher<*>)
    : org.junit.AssumptionViolatedException(assumption), SelfDescribing {

    override val message: String?
        get() = description

    override val description: String
        get() {
            val builder = StringBuilder()

            if (assumption != null) {
                builder.append(assumption).append(": ")
            }

            builder.append("got: ").append(describe(actual))
                    .append(", expected: ").append(describe(matcher))

            return builder.toString()
        }

    companion object {
        fun <T> create(actual: T, matcher: Matcher<T>): AssumptionViolatedException {
            return AssumptionViolatedException(actual = actual, matcher = matcher)
        }

        fun <T> create(message: String, actual: T, matcher: Matcher<T>): AssumptionViolatedException {
            return AssumptionViolatedException(assumption = message, actual = actual, matcher = matcher)
        }
    }
}