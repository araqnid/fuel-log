package org.araqnid.fuellog.integration

import org.apache.http.nio.client.HttpAsyncClient
import org.araqnid.fuellog.FacebookClient
import org.araqnid.fuellog.FacebookClientConfig
import org.hamcrest.Matchers
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.Assume.assumeThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import javax.ws.rs.BadRequestException
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
    fun fetches_app_token() {
        assumeThat(facebookAppId, not(isEmptyString()))
        assumeThat(facebookAppSecret, not(isEmptyString()))

        val facebookClient = FacebookClient(server.instance<FacebookClientConfig>(), server.instance<HttpAsyncClient>())
        val appToken = facebookClient.fetchFacebookAppToken().toCompletableFuture().join()
        assertNotEquals("", appToken)
    }

    @Test
    fun failure_to_fetch_app_token_produces_bad_request_exception() {
        val facebookClient = FacebookClient(FacebookClientConfig("", ""), server.instance<HttpAsyncClient>())

        expected.expectCause(Matchers.instanceOf(BadRequestException::class.java))
        facebookClient.fetchFacebookAppToken().toCompletableFuture().join()
    }
}
