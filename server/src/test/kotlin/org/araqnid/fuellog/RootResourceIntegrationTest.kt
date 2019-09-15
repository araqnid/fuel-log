package org.araqnid.fuellog

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.or
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.araqnid.hamkrest.json.json
import org.araqnid.hamkrest.json.jsonNull
import org.araqnid.hamkrest.json.jsonObject
import org.araqnid.hamkrest.json.jsonString
import org.junit.Test
import javax.ws.rs.core.MediaType

class RootResourceIntegrationTest : IntegrationTest() {

    @Test fun root_returns_api_info_and_user_identity() {
        val user = loginAsNewUser()
        execute(HttpGet(server.uri("/_api/")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType(MediaType.APPLICATION_JSON))
        assertThat(response.entity.text,
                json(jsonObject()
                        .withProperty("version", jsonNull() or jsonString(anything))
                        .withProperty("google_maps_api_key", "xxx")
                        .withPropertyJSON("user_info", "{ realm: 'TEST', user_id: '${user.userId}', name: '${user.name}', picture: null }")))
    }
}
