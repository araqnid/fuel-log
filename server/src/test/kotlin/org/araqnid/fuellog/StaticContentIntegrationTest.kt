package org.araqnid.fuellog

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.Test

class StaticContentIntegrationTest : IntegrationTest() {
    @Test fun server_serves_static_content() {
        val response = execute(HttpGet(server.uri("/index.html")))
        assertThat(response, Matcher(HttpResponse::isSuccess))
        assertThat(response.entity, hasMimeType("text/html"))
    }

    @Test fun server_serves_static_content_as_default_page() {
        val response = execute(HttpGet(server.uri("/")))
        assertThat(response, Matcher(HttpResponse::isSuccess))
        assertThat(response.entity, hasMimeType("text/html"))
    }
}
