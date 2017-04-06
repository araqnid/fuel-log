package org.araqnid.fuellog.integration

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.araqnid.fuellog.matchers.jsonAnyString
import org.araqnid.fuellog.matchers.jsonNull
import org.araqnid.fuellog.matchers.jsonObject
import org.araqnid.fuellog.matchers.jsonTextStructuredAs
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.either
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import javax.ws.rs.core.MediaType

class RootResourceIntegrationTest : IntegrationTest() {

    @Test fun root_has_json() {
        execute(HttpGet(server.uri("/_api/")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.APPLICATION_JSON))
        assertThat(response.entity.text,
                jsonTextStructuredAs(jsonObject()
                        .withProperty("version", either(jsonNull()).or(jsonAnyString()))
                        .withProperty("user_info", jsonNull())))
    }

    @Test fun root_returns_current_user() {
        val user = loginAsNewUser()
        execute(HttpGet(server.uri("/_api/")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.APPLICATION_JSON))
        assertThat(response.entity.text,
                jsonTextStructuredAs(jsonObject()
                        .withProperty("version", either(jsonNull()).or(jsonAnyString()))
                        .withPropertyJSON("user_info", "{ realm: 'TEST', user_id: '${user.userId}', name: '${user.name}', picture: null }")))
    }
}