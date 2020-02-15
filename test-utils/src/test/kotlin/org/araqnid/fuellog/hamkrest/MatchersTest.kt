package org.araqnid.fuellog.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class MatchersTest {
    @Test
    fun `contains only`() {
        assertThat(containsOnly(equalTo("alpha"))(setOf("alpha")), matches)
        assertThat(containsOnly(equalTo("alpha"))(setOf()), !matches)
        assertThat(containsOnly(equalTo("alpha"))(setOf("alpha", "beta")), !matches)
    }

    @Test
    fun `contains in order`() {
        assertThat(containsInOrder(equalTo("alpha"), equalTo("beta"))(listOf("alpha", "beta")), matches)
        assertThat(containsInOrder(equalTo("alpha"), equalTo("beta"))(listOf("alpha", "beta", "gamma")), !matches)
        assertThat(containsInOrder(equalTo("alpha"), equalTo("beta"))(listOf("alpha")), !matches)
        assertThat(containsInOrder(equalTo("alpha"), equalTo("beta"))(listOf("beta", "alpha")), !matches)
    }

    @Test
    fun `contains in any order`() {
        assertThat(containsInAnyOrder(equalTo("alpha"), equalTo("beta"))(listOf("alpha", "beta")), matches)
        assertThat(containsInAnyOrder(equalTo("alpha"), equalTo("beta"))(listOf("alpha", "beta", "gamma")), !matches)
        assertThat(containsInAnyOrder(equalTo("alpha"), equalTo("beta"))(listOf("alpha")), !matches)
        assertThat(containsInAnyOrder(equalTo("alpha"), equalTo("beta"))(listOf("beta", "alpha")), matches)
    }
}

private val matches: Matcher<MatchResult> = equalTo(MatchResult.Match)
