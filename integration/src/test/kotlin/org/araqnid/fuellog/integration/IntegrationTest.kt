package org.araqnid.fuellog.integration

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.ByteStreams
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.HttpVersion
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicNameValuePair
import org.araqnid.fuellog.IdentityResources
import org.junit.Rule
import java.util.UUID

abstract class IntegrationTest {
    @Rule @JvmField val server = ServerRunner(mapOf(
            "PORT" to "0",
            "DOCUMENT_ROOT" to "../ui/build/site",
            "FACEBOOK_APP_ID" to "",
            "FACEBOOK_APP_SECRET" to "",
            "GOOGLE_CLIENT_ID" to "",
            "GOOGLE_CLIENT_SECRET" to "",
            "GOOGLE_MAPS_API_KEY" to "xxx"
    ))

    var response: HttpResponse = BasicHttpResponse(HttpVersion.HTTP_1_0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "No request executed")
    var currentUser: IdentityResources.UserInfo? = null
    val httpContext = HttpClientContext()

    fun execute(request: HttpUriRequest) {
        response = server.client.execute(request, { rawResponse ->
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

    fun loginAsNewUser(): IdentityResources.UserInfo {
        val userName = "Test User"
        execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to UUID.randomUUID().toString(), "name" to userName))
        })
        val userInfo = userInfoReader.readValue<IdentityResources.UserInfo>(response.entity.content)
        currentUser = userInfo
        return userInfo
    }
}

internal fun formEntity(params: Map<String, String>) = UrlEncodedFormEntity(params.map { (k, v) -> BasicNameValuePair(k, v) }.toList())

internal val userInfoReader = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .readerFor(IdentityResources.UserInfo::class.java)
