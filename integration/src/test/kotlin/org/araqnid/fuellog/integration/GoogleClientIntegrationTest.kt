package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import kotlinx.coroutines.runBlocking
import org.araqnid.fuellog.AppConfig
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
        val googleClient = GoogleClient(AppConfig.GOOGLE_TOKEN_INFO_ENDPOINT, googleClientConfig, httpClient, clock)
        val tokenInfo = runBlocking { googleClient.validateToken(googleIdToken) }
        assertThat(tokenInfo, has(GoogleClient.TokenInfo::clientId, equalTo(googleClientConfig.id)))
    }

    @Test
    fun `failure to validate ID token produces BadRequestException`() {
        val googleClient =
            GoogleClient(AppConfig.GOOGLE_TOKEN_INFO_ENDPOINT, GoogleClientConfig("", ""), httpClient, clock)

        assertThrows<BadRequestException> {
            runBlocking { googleClient.validateToken("") }
        }
    }
}
