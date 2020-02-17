package org.araqnid.fuellog

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.matches
import org.junit.Test

class BasicStatusComponentsTest {
    @Test
    fun `jvm version is populated`() {
        assertThat(BasicStatusComponents.jvmVersion, anything)
    }

    @Test
    fun `jetty version is populated`() {
        assertThat(BasicStatusComponents.jettyVersion, anything)
    }

    @Test
    fun `kotlin version is populated`() {
        assertThat(BasicStatusComponents.kotlinVersion, matches(Regex("\\d+\\.\\d+\\.\\d+")))
    }
}
