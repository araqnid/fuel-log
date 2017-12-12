package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.araqnid.hamkrest.json.equivalentTo
import org.junit.Test

class PreferencesResourcesIntegrationTest : IntegrationTest() {
    @Test fun identity_resource_forbidden_for_unauthenticated_user() {
        execute(HttpGet("/_api/user/preferences"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_FORBIDDEN))
    }

    @Test fun returns_default_preferences_for_user() {
        loginAsNewUser()
        execute(HttpGet("/_api/user/preferences"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType("application/json"))
        assertThat(response.entity.text, equivalentTo("{ fuel_volume_unit: 'LITRES', distance_unit: 'MILES', currency: 'GBP' }"))
    }
}