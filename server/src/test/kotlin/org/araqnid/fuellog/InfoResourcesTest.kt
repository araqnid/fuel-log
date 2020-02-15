package org.araqnid.fuellog

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.araqnid.hamkrest.json.jsonObject
import org.araqnid.hamkrest.json.jsonString
import org.junit.Rule
import org.junit.Test
import javax.ws.rs.core.MediaType

class InfoResourcesTest {
    @get:Rule
    val server = ServerRunner()

    @Test
    fun readiness_has_text() {
        val response = server.execute(HttpGet(server.uri("/_api/info/readiness")).accepting(MediaType.TEXT_PLAIN))
        assertThat(response, Matcher(HttpResponse::isSuccess))
        assertThat(response.entity, hasMimeType(MediaType.TEXT_PLAIN))
        assertThat(response.entity.text, equalTo("READY"))
    }

    @Test
    fun version_has_json() {
        val response = server.execute(HttpGet(server.uri("/_api/info/version")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response, isJsonOk(jsonObject()
                        .withProperty("version", jsonScalar())
                        .withProperty("title", jsonScalar())
                        .withProperty("vendor", jsonScalar())))
    }

    @Test fun version_has_text() {
        val response = server.execute(HttpGet(server.uri("/_api/info/version")).accepting(MediaType.TEXT_PLAIN))
        assertThat(response, Matcher(HttpResponse::isSuccess))
        assertThat(response.entity, hasMimeType(MediaType.TEXT_PLAIN))
        assertThat(response.entity.text, anything)
    }

    @Test fun status_has_json() {
        val response = server.execute(HttpGet(server.uri("/_api/info/status")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response, isJsonOk(jsonObject()
                .withProperty("status", jsonString(anything))
                .withProperty("components", jsonObject()
                        .withProperty("jvmVersion", jsonObject().withAnyOtherProperties())
                        .withAnyOtherProperties()
                )))
    }
}
