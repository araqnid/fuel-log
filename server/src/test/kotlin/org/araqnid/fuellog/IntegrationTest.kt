package org.araqnid.fuellog

import com.google.common.io.ByteStreams
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.message.BasicHttpResponse
import org.junit.Rule
import java.util.UUID

abstract class IntegrationTest {
    @Rule @JvmField val server = ServerRunner(mapOf(
            "PORT" to "0",
            "FACEBOOK_APP_ID" to "",
            "FACEBOOK_APP_SECRET" to "",
            "GOOGLE_CLIENT_ID" to "",
            "GOOGLE_CLIENT_SECRET" to "",
            "GOOGLE_MAPS_API_KEY" to "xxx"
    ))

    lateinit var currentUser: IdentityResources.UserInfo
    val httpContext = HttpClientContext()

    fun execute(request: HttpUriRequest): HttpResponse {
        return server.client.execute(request, { rawResponse ->
            val bufferedResponse = BasicHttpResponse(rawResponse.statusLine)
            rawResponse.allHeaders.forEach { bufferedResponse.addHeader(it) }
            if (rawResponse.entity != null) {
                val bytes = ByteStreams.toByteArray(rawResponse.entity.content)
                bufferedResponse.entity = ByteArrayEntity(bytes).apply {
                    contentType = rawResponse.entity.contentType
                }
            }
            bufferedResponse
        }, httpContext)
    }

    fun loginAsNewUser() {
        val userName = "Test User"
        val response = execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to UUID.randomUUID().toString(), "name" to userName))
        })
        currentUser = response.readJson()
    }
}
