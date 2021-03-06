package org.araqnid.fuellog

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StaticContentTest {
    @get:Rule
    val server = ServerRunner()

    @Before
    fun `create index file`() {
        server.webContentFolder.newFile("index.html").toFile().writeText("hello world")
    }

    @Test
    fun `server serves static content`() {
        val response = server.execute(HttpGet(server.uri("/index.html")))
        assertThat(response, Matcher(HttpResponse::isSuccess))
        assertThat(response.entity, hasMimeType("text/html"))
    }

    @Test fun `server serves static content as default page`() {
        val response = server.execute(HttpGet(server.uri("/")))
        assertThat(response, Matcher(HttpResponse::isSuccess))
        assertThat(response.entity, hasMimeType("text/html"))
    }
}
