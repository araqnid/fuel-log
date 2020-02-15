package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.isEmptyString
import kotlinx.coroutines.runBlocking
import org.araqnid.fuellog.FacebookClient
import org.araqnid.fuellog.FacebookClientConfig
import org.araqnid.fuellog.hamkrest.assumeThat
import org.araqnid.fuellog.hamkrest.expect
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import javax.ws.rs.BadRequestException
import kotlin.test.assertNotEquals

class FacebookClientIntegrationTest {
    @get:Rule
    val httpClient = HttpAsyncClientRule()

    @get:Rule
    val expected = ExpectedException.none()

    private val facebookClientConfig by lazy { FacebookClientConfig(facebookAppId, facebookAppSecret) }

    @Test
    fun `fetches app token`() {
        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val appToken = runBlocking { facebookClient.fetchFacebookAppToken() }
        assertNotEquals("", appToken)
    }

    @Test
    fun `failure to fetch app token produces bad request exception`() {
        val facebookClient = FacebookClient(FacebookClientConfig("", ""), httpClient)

        expected.expect(isA<BadRequestException>())
        runBlocking { facebookClient.fetchFacebookAppToken() }
    }

    @Test
    fun `fetches user profile by id`() {
        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val result = runBlocking { facebookClient.fetchUserProfile("10155233566049669") }

        assertThat(result, has(FacebookClient.UserIdentity::id, equalTo("10155233566049669")))
    }

    @Test
    fun `fetches user's own profile`() {
        assumeThat(accessToken, !isEmptyString)

        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val result = runBlocking { facebookClient.fetchUsersOwnProfile(accessToken) }

        assertThat(result, has(FacebookClient.UserIdentity::id, equalTo("10155233566049669")))
    }

    @Test
    fun `validates user access token`() {
        assumeThat(accessToken, !isEmptyString)

        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val result = runBlocking { facebookClient.validateUserAccessToken(accessToken) }

        assertThat(result, has(FacebookClient.DebugTokenResponse::appId, equalTo(facebookClientConfig.id))
                and has(FacebookClient.DebugTokenResponse::type, equalTo("USER"))
                and has(FacebookClient.DebugTokenResponse::isValid, equalTo(true)))
    }
}
