package org.araqnid.fuellog.integration

import org.apache.http.nio.client.HttpAsyncClient
import org.araqnid.fuellog.GoogleClient
import org.araqnid.fuellog.GoogleClientConfig
import org.hamcrest.Matchers
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.Assume.assumeThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import javax.ws.rs.BadRequestException

class GoogleClientIntegrationTest {
    companion object {
        val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: ""
        val googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET") ?: ""
    }

    @get:Rule
    val server = ServerRunner(mapOf("PORT" to "0", "DOCUMENT_ROOT" to "../ui/build/site",
            "FACEBOOK_APP_ID" to "", "FACEBOOK_APP_SECRET" to "",
            "GOOGLE_CLIENT_ID" to googleClientId, "GOOGLE_CLIENT_SECRET" to googleClientSecret))

    @get:Rule
    val expected = ExpectedException.none()

    @Ignore("don't actually have a way of testing this, need a test id token")
    @Test
    fun validates_id_token() {
        assumeThat(googleClientId, not(isEmptyString()))
        assumeThat(googleClientSecret, not(isEmptyString()))

        val googleClient = GoogleClient(server.instance<GoogleClientConfig>(), server.instance<HttpAsyncClient>(), server.clock)
        googleClient.validateToken("").toCompletableFuture().join()
    }

    @Test
    fun failure_to_validate_id_token_produces_bad_request_exception() {
        val googleClient = GoogleClient(GoogleClientConfig("", ""), server.instance<HttpAsyncClient>(), server.clock)

        expected.expectCause(Matchers.instanceOf(BadRequestException::class.java))
        googleClient.validateToken("").toCompletableFuture().join()
    }
}