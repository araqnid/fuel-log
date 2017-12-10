package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.isEmptyString
import kotlinx.coroutines.experimental.future.future
import org.apache.http.nio.client.HttpAsyncClient
import org.araqnid.fuellog.FacebookClient
import org.araqnid.fuellog.FacebookClientConfig
import org.araqnid.fuellog.hamkrest.assumeThat
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.net.URI
import javax.ws.rs.BadRequestException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FacebookClientIntegrationTest {
    companion object {
        val facebookAppId = System.getenv("FACEBOOK_APP_ID") ?: ""
        val facebookAppSecret = System.getenv("FACEBOOK_APP_SECRET") ?: ""
    }

    @get:Rule
    val server = ServerRunner(mapOf("PORT" to "0", "DOCUMENT_ROOT" to "../ui/build/site",
            "FACEBOOK_APP_ID" to facebookAppId, "FACEBOOK_APP_SECRET" to facebookAppSecret,
            "GOOGLE_CLIENT_ID" to "", "GOOGLE_CLIENT_SECRET" to ""))

    @get:Rule
    val expected = ExpectedException.none()

    @Test
    fun `fetches app token`() {
        assumeThat(facebookAppId, !isEmptyString)
        assumeThat(facebookAppSecret, !isEmptyString)

        val facebookClient = FacebookClient(server.instance<FacebookClientConfig>(), server.instance<HttpAsyncClient>())
        val appToken = future { facebookClient.fetchFacebookAppToken() }.join()
        assertNotEquals("", appToken)
    }

    @Test
    fun `failure to fetch app token produces bad request exception`() {
        val facebookClient = FacebookClient(FacebookClientConfig("", ""), server.instance<HttpAsyncClient>())

        expected.expectCause(Matchers.instanceOf(BadRequestException::class.java))
        future { facebookClient.fetchFacebookAppToken() }.join()
    }

    // tests that can be performed only with a current, valid, user access token
    val accessToken = ""

    @Test
    fun `fetches user profile by id`() {
        assumeThat(facebookAppId, !isEmptyString)
        assumeThat(facebookAppSecret, !isEmptyString)
        assumeThat(accessToken, !isEmptyString)

        val facebookClient = FacebookClient(server.instance<FacebookClientConfig>(), server.instance<HttpAsyncClient>())
        val result = future { facebookClient.fetchUserProfile("10155233566049669") }.join()
        assertEquals(FacebookClient.UserIdentity("Steve Haslam", "10155233566049669", FacebookClient.Picture(FacebookClient.PictureData(50, 50, URI.create("https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/22365519_10155849960609669_4611817305999182053_n.jpg?oh=dbbd4324f5adf21776510ed0cd5dd3b9&oe=5A9F07D2"), false))), result)
    }

    @Test
    fun `fetches user's own profile`() {
        assumeThat(facebookAppId, !isEmptyString)
        assumeThat(facebookAppSecret, !isEmptyString)
        assumeThat(accessToken, !isEmptyString)

        val facebookClient = FacebookClient(server.instance<FacebookClientConfig>(), server.instance<HttpAsyncClient>())
        val result = future { facebookClient.fetchUsersOwnProfile(accessToken) }.join()
        assertEquals(FacebookClient.UserIdentity("Steve Haslam", "10155233566049669", FacebookClient.Picture(FacebookClient.PictureData(50, 50, URI.create("https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/22365519_10155849960609669_4611817305999182053_n.jpg?oh=dbbd4324f5adf21776510ed0cd5dd3b9&oe=5A9F07D2"), false))), result)
    }
}
