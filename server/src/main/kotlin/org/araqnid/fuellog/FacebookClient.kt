package org.araqnid.fuellog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicNameValuePair
import org.apache.http.nio.client.HttpAsyncClient
import java.net.URI
import java.time.Instant
import javax.ws.rs.BadRequestException

class FacebookClient(private val config: FacebookClientConfig, private val asyncHttpClient: HttpAsyncClient) {
    private val debugTokenUri = URI("https://graph.facebook.com/debug_token")
    private val oauthAccessTokenUri = URI("https://graph.facebook.com/oauth/access_token")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AccessTokenResponse(val accessToken: String, val tokenType: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DebugTokenResponse(val userId: String, val type: String, val appId: String, val application: String,
                                  val expiresAt: Instant, val isValid: Boolean, val scopes: Set<String>)

    data class UserIdentity(val name: String, val id: String, val picture: Picture)
    data class Picture(val data: PictureData)
    data class PictureData(val width: Int,
                           val height: Int,
                           val url: URI, @JsonProperty("is_silhouette") val silhouette: Boolean)

    private val objectMapperForFacebookEndpoint = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    suspend fun fetchFacebookAppToken(): String {
        val request = HttpGet(oauthAccessTokenUri.withParameters(
                "client_id" to config.id,
                "client_secret" to config.secret,
                "grant_type" to "client_credentials"
        ))

        val response = asyncHttpClient.executeAsyncHttpRequest(request)

        return parseJsonResponse<AccessTokenResponse>(oauthAccessTokenUri, response).accessToken
    }

    suspend fun validateUserAccessToken(token: String): DebugTokenResponse {
        val request = HttpGet(debugTokenUri.withParameters("input_token" to token,
                "access_token" to "${config.id}|${config.secret}"))

        val response = asyncHttpClient.executeAsyncHttpRequest(request)

        return parseJsonResponse(debugTokenUri, response) { withRootName("data") }
    }

    suspend fun fetchUsersOwnProfile(accessToken: String): UserIdentity {
        val uri = URI("https://graph.facebook.com/me")
        val response = asyncHttpClient.executeAsyncHttpRequest(HttpGet(uri.withParameters(
                "access_token" to accessToken,
                "fields" to "id,name,picture"
        )))
        return parseJsonResponse(uri, response)
    }

    suspend fun fetchUserProfile(userId: String): UserIdentity {
        val uri = URI("https://graph.facebook.com/$userId")
        val response = asyncHttpClient.executeAsyncHttpRequest(HttpGet(uri.withParameters(
                "access_token" to "${config.id}|${config.secret}",
                "fields" to "id,name,picture"
        )))
        return parseJsonResponse(uri, response)
    }

    private val permittedMimeTypes = setOf("text/javascript", "application/json")

    private inline fun <reified T : Any> parseJsonResponse(uri: URI,
                                                           response: HttpResponse,
                                                           configureReader: ObjectReader.() -> ObjectReader = { this }): T {
        if (response.statusLine.statusCode != HttpStatus.SC_OK)
            throw BadRequestException("$uri: ${response.statusLine}")

        val contentType = ContentType.get(response.entity)
        if (contentType.mimeType.toLowerCase() !in permittedMimeTypes)
            throw BadRequestException("$uri: unhandled content-type: $contentType")

        return objectMapperForFacebookEndpoint.readerFor(jacksonTypeRef<T>()).let(configureReader).readValue<T>(response.entity.content)
    }

    private fun URI.withParameters(vararg parameters: Pair<String, String>): URI =
            URIBuilder(this).setParameters(parameters.map { BasicNameValuePair(it.first, it.second) }).build()
}
