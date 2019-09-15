package org.araqnid.fuellog

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.araqnid.hamkrest.json.bytesEquivalentTo
import org.junit.Test

class PreferencesResourcesIntegrationTest : IntegrationTest() {
    @Test fun identity_resource_forbidden_for_unauthenticated_user() {
        val response = execute(HttpGet("/_api/user/preferences"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_FORBIDDEN))
    }

    @Test fun returns_default_preferences_for_user() {
        loginAsNewUser()
        val response = execute(HttpGet("/_api/user/preferences"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType("application/json"))
        assertThat(response.entity.bytes, bytesEquivalentTo("{ fuel_volume_unit: 'LITRES', distance_unit: 'MILES', currency: 'GBP' }"))
    }
}
