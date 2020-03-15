package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import kotlinx.coroutines.runBlocking
import org.araqnid.fuellog.GoogleClient
import org.araqnid.fuellog.GoogleClientConfig
import org.araqnid.fuellog.httpClient
import org.araqnid.fuellog.test.assertThrows
import org.junit.Test
import java.time.Clock
import javax.ws.rs.BadRequestException

class GoogleClientIntegrationTest {
    val httpClient = httpClient { }

    private val clock = Clock.systemDefaultZone()

    private val googleClientConfig by lazy { GoogleClientConfig(googleClientId, googleClientSecret) }

    @Test
    fun `validates ID token`() {
        val googleClient = GoogleClient(googleClientConfig, httpClient, clock)
        val tokenInfo = runBlocking { googleClient.validateToken(googleIdToken) }
        assertThat(tokenInfo, has(GoogleClient.TokenInfo::clientId, equalTo(googleClientConfig.id)))
    }

    @Test
    fun `failure to validate ID token produces BadRequestException`() {
        val googleClient = GoogleClient(GoogleClientConfig("", ""), httpClient, clock)

        assertThrows<BadRequestException> {
            runBlocking { googleClient.validateToken("") }
        }
    }
}
