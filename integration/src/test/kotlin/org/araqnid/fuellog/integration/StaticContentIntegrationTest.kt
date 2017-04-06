package org.araqnid.fuellog.integration

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.araqnid.eventstore.EventSource
import org.araqnid.eventstore.InMemoryEventSource
import org.araqnid.fuellog.integration.IntegrationTest
import org.araqnid.fuellog.integration.hasMimeType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Test

class StaticContentIntegrationTest : IntegrationTest() {
    @Test fun server_serves_static_content() {
        execute(HttpGet(server.uri("/index.html")))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType("text/html"))
    }

    @Test fun server_serves_static_content_as_default_page() {
        execute(HttpGet(server.uri("/")))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType("text/html"))
    }
}