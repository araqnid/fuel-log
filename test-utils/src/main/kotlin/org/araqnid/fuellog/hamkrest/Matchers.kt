package org.araqnid.fuellog.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe

fun <T> containsOnly(matcher: Matcher<T>): Matcher<Collection<T>> {
    return object : Matcher.Primitive<Collection<T>>() {
        override fun invoke(actual: Collection<T>): MatchResult {
            if (actual.isEmpty())
                return MatchResult.Mismatch("was empty")
            if (actual.size > 1)
                return MatchResult.Mismatch("contained multiple elements: ${describe(actual)}")
            return matcher(actual.first())
        }

        override val description: String
            get() = "in which the only element ${describe(matcher)}"
    }
}

fun <T> containsInOrder(vararg matchers: Matcher<T>): Matcher<Collection<T>> {
    return object : Matcher.Primitive<Collection<T>>() {
        override fun invoke(actual: Collection<T>): MatchResult {
            val expectedIter = matchers.iterator()
            val actualIter = actual.iterator()
            var index = 0
            while (expectedIter.hasNext() && actualIter.hasNext()) {
                val expectedMatcher = expectedIter.next()
                val actualValue = actualIter.next()
                val result = expectedMatcher(actualValue)
                if (result is MatchResult.Mismatch) {
                    return MatchResult.Mismatch("at index $index: ${describe(result)}")
                }
                ++index
            }
            if (expectedIter.hasNext())
                return MatchResult.Mismatch("expected more than $index values")
            if (actualIter.hasNext())
                return MatchResult.Mismatch("had more than $index values: ${describe(actualIter.asSequence().toList())}")
            return MatchResult.Match
        }

        override val description: String
            get() = "contains in order: ${describe(matchers.toList())}"
    }
}

fun <T> containsInAnyOrder(vararg matchers: Matcher<T>): Matcher<Collection<T>> {
    return object : Matcher.Primitive<Collection<T>>() {
        override fun invoke(actual: Collection<T>): MatchResult {
            for ((actualIndex, actualValue) in actual.withIndex()) {
                val matched = mutableListOf<Matcher<T>>()
                for (matcher in matchers) {
                    val result = matcher(actualValue)
                    if (result == MatchResult.Match) {
                        matched.add(matcher)
                    }
                }
                if (matched.isEmpty())
                    return MatchResult.Mismatch("element at $actualIndex did not satisfy any matcher: ${describe(actualValue)}")
                else if (matched.size > 1)
                    return MatchResult.Mismatch("element at $actualIndex matched multiple matchers: ${describe(actualValue)}")
            }
            return MatchResult.Match
        }

        override val description: String
            get() = "contains in any order: ${describe(matchers.toList())}"
    }
}
