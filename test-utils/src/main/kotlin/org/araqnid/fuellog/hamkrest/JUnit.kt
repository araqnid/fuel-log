package org.araqnid.fuellog.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.SelfDescribing
import com.natpryce.hamkrest.describe
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.rules.ExpectedException

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
        get() = buildString {
            if (assumption != null) {
                append(assumption)
                append(": ")
            }

            append("got: ")
            append(describe(actual))
            append(", expected: ")
            append(describe(matcher))
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

fun <T> Matcher<T>.asHamcrest(): org.hamcrest.Matcher<T> {
    return object : TypeSafeDiagnosingMatcher<T>() {
        override fun matchesSafely(item: T, mismatchDescription: Description): Boolean {
            val result = this@asHamcrest.invoke(item)
            if (result == MatchResult.Match) {
                return true
            }
            else {
                mismatchDescription.appendText(describe(result))
                return false
            }
        }

        override fun describeTo(description: Description) {
            description.appendText(this@asHamcrest.description)
        }
    }
}

fun ExpectedException.expect(exception: Matcher<Throwable>) {
    expect(exception.asHamcrest())
}

fun ExpectedException.expectCause(cause: Matcher<Throwable>) {
    expectCause(cause.asHamcrest())
}
