package org.araqnid.fuellog

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.or
import org.apache.http.client.methods.HttpGet
import org.araqnid.hamkrest.json.jsonNull
import org.araqnid.hamkrest.json.jsonObject
import org.araqnid.hamkrest.json.jsonString
import org.junit.Test
import javax.ws.rs.core.MediaType

class RootResourceIntegrationTest : IntegrationTest() {

    @Test fun root_returns_api_info_and_user_identity() {
        loginAsNewUser()
        val response = execute(HttpGet(server.uri("/_api/")).accepting(MediaType.APPLICATION_JSON))
        assertThat(response, isJsonOk(jsonObject()
                .withProperty("version", jsonNull() or jsonString(anything))
                .withProperty("google_maps_api_key", "xxx")
                .withPropertyJSON("user_info",
                        "{ realm: 'TEST', user_id: '${currentUser.userId}', name: '${currentUser.name}', picture: null }")))
    }
}
