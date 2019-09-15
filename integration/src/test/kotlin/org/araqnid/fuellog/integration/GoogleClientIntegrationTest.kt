package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.runBlocking
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.araqnid.fuellog.GoogleClient
import org.araqnid.fuellog.GoogleClientConfig
import org.araqnid.fuellog.hamkrest.expect
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.time.Clock
import javax.ws.rs.BadRequestException

class GoogleClientIntegrationTest {
    @Before
    fun startHttpClient() {
        httpClient.start()
    }

    @After
    fun cleanupHttpClient() {
        httpClient.close()
    }

    @get:Rule
    val expected = ExpectedException.none()

    private val clock = Clock.systemDefaultZone()

    private val httpClient = HttpAsyncClients.createDefault()

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

        expected.expect(isA<BadRequestException>())

        runBlocking { googleClient.validateToken("") }
    }
}
