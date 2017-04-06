package org.araqnid.fuellog.integration

import com.fasterxml.jackson.databind.JsonNode
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.araqnid.fuellog.matchers.jsonBytesStructuredAs
import org.araqnid.fuellog.matchers.jsonNull
import org.araqnid.fuellog.matchers.jsonObject
import org.araqnid.fuellog.matchers.jsonString
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.any
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import javax.ws.rs.core.MediaType

class InfoResourcesIntegrationTest : IntegrationTest() {
    @Test fun readiness_has_text() {
        execute(HttpGet(server.uri("/_api/info/readiness")).accepting(MediaType.TEXT_PLAIN))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.TEXT_PLAIN))
        assertThat(response.entity.text, equalTo("READY"))
    }

    @Test fun version_has_json() {
        execute(HttpGet(server.uri("/_api/info/version")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.APPLICATION_JSON))
        assertThat(response.entity.bytes, jsonBytesStructuredAs(jsonObject()
                        .withProperty("version", any(JsonNode::class.java))
                        .withProperty("title", "fuel-log")
                        .withProperty("vendor", jsonNull())))
    }

    @Test fun version_has_text() {
        execute(HttpGet(server.uri("/_api/info/version")).accepting(MediaType.TEXT_PLAIN))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.TEXT_PLAIN))
        assertThat(response.entity.text, any(String::class.java))
    }

    @Test fun status_has_json() {
        execute(HttpGet(server.uri("/_api/info/status")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.APPLICATION_JSON))
        assertThat(response.entity.bytes,
                jsonBytesStructuredAs(jsonObject()
                        .withProperty("status", jsonString(any(String::class.java)))
                        .withProperty("components", jsonObject()
                                .withPropertyJSON("jvmVersion", "{ priority: 'INFO', label: 'JVM version', text: '${System.getProperty("java.version")}' }")
                                .withAnyOtherProperties()
                        )))
    }
}
