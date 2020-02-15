package org.araqnid.fuellog

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.junit.Rule
import org.junit.Test

class PreferencesResourcesTest {
    @get:Rule
    val server = ServerRunner()

    @Test
    fun identity_resource_forbidden_for_unauthenticated_user() {
        val response = server.execute(HttpGet("/_api/user/preferences"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_FORBIDDEN))
    }

    @Test
    fun returns_default_preferences_for_user() {
        server.loginAsNewUser()
        val response = server.execute(HttpGet("/_api/user/preferences"))
        assertThat(response, isJsonOk("{ fuel_volume_unit: 'LITRES', distance_unit: 'MILES', currency: 'GBP' }"))
    }
}
