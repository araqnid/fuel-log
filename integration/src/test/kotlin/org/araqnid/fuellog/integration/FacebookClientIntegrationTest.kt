package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmptyString
import com.natpryce.hamkrest.present
import kotlinx.coroutines.experimental.future.future
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.araqnid.fuellog.FacebookClient
import org.araqnid.fuellog.FacebookClientConfig
import org.araqnid.fuellog.hamkrest.assumeThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import javax.ws.rs.BadRequestException
import kotlin.test.assertNotEquals

class FacebookClientIntegrationTest {
    companion object {
        val facebookAppId = System.getenv("FACEBOOK_APP_ID") ?: ""
        val facebookAppSecret = System.getenv("FACEBOOK_APP_SECRET") ?: ""

        // some tests can be performed only with a current, valid, user access token
        val accessToken = System.getenv("FACEBOOK_USER_ACCESS_TOKEN") ?: ""
    }

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

    private val httpClient = HttpAsyncClients.createDefault()

    private val facebookClientConfig: FacebookClientConfig
        get() {
            assumeThat(facebookAppId, !isEmptyString)
            assumeThat(facebookAppSecret, !isEmptyString)
            return FacebookClientConfig(facebookAppId, facebookAppSecret)
        }

    @Test
    fun `fetches app token`() {
        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val appToken = future { facebookClient.fetchFacebookAppToken() }.join()
        assertNotEquals("", appToken)
    }

    @Test
    fun `failure to fetch app token produces bad request exception`() {
        val facebookClient = FacebookClient(FacebookClientConfig("", ""), httpClient)

        expected.expectCause(Matchers.instanceOf(BadRequestException::class.java))
        future { facebookClient.fetchFacebookAppToken() }.join()
    }

    @Test
    fun `fetches user profile by id`() {
        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val result = future { facebookClient.fetchUserProfile("10155233566049669") }.join()

        assertThat(result, has(FacebookClient.UserIdentity::name, equalTo("Steve Haslam"))
                and has(FacebookClient.UserIdentity::id, equalTo("10155233566049669"))
                and has(FacebookClient.UserIdentity::picture, present()))
    }

    @Test
    fun `fetches user's own profile`() {
        assumeThat(accessToken, !isEmptyString)

        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val result = future { facebookClient.fetchUsersOwnProfile(accessToken) }.join()

        assertThat(result, has(FacebookClient.UserIdentity::name, equalTo("Steve Haslam"))
                and has(FacebookClient.UserIdentity::id, equalTo("10155233566049669"))
                and has(FacebookClient.UserIdentity::picture, present()))
    }

    @Test
    fun `validates user access token`() {
        assumeThat(accessToken, !isEmptyString)

        val facebookClient = FacebookClient(facebookClientConfig, httpClient)
        val result = future { facebookClient.validateUserAccessToken(accessToken) }.join()

        assertThat(result, has(FacebookClient.DebugTokenResponse::appId, equalTo(facebookClientConfig.id))
                and has(FacebookClient.DebugTokenResponse::type, equalTo("USER"))
                and has(FacebookClient.DebugTokenResponse::isValid, equalTo(true)))
    }
}
