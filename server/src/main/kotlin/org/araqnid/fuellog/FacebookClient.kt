package org.araqnid.fuellog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.nio.client.HttpAsyncClient
import java.net.URI
import java.time.Instant
import javax.inject.Inject

class FacebookClient @Inject constructor(
    private val config: FacebookClientConfig,
    private val asyncHttpClient: HttpAsyncClient
) {
    private val debugTokenUri = URI("https://graph.facebook.com/debug_token")
    private val oauthAccessTokenUri = URI("https://graph.facebook.com/oauth/access_token")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AccessTokenResponse(val accessToken: String, val tokenType: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DebugTokenResponse(
        val userId: String, val type: String, val appId: String, val application: String,
        val expiresAt: Instant, val isValid: Boolean, val scopes: Set<String>
    )

    data class UserIdentity(val name: String, val id: String, val picture: Picture)
    data class Picture(val data: PictureData)
    data class PictureData(val width: Int,
                           val height: Int,
                           val url: URI, @JsonProperty("is_silhouette") val silhouette: Boolean)

    private val objectMapperForFacebookEndpoint = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    private val accessTokenResponseReader = objectMapperForFacebookEndpoint.readerFor(AccessTokenResponse::class.java)
    private val debugTokenResponseReader = objectMapperForFacebookEndpoint.readerFor(DebugTokenResponse::class.java)
            .withRootName("data")
    private val userIdentityReader = objectMapperForFacebookEndpoint.readerFor(UserIdentity::class.java)

    suspend fun fetchFacebookAppToken(): String {
        val request = HttpGet(oauthAccessTokenUri.withParameters(
                "client_id" to config.id,
                "client_secret" to config.secret,
                "grant_type" to "client_credentials"
        ))

        val response = asyncHttpClient.executeAsyncHttpRequest(request)

        return parseJsonResponse<AccessTokenResponse>(oauthAccessTokenUri,
                response,
                accessTokenResponseReader).accessToken
    }

    suspend fun validateUserAccessToken(token: String): DebugTokenResponse {
        val request = HttpGet(debugTokenUri.withParameters("input_token" to token,
                "access_token" to "${config.id}|${config.secret}"))

        val response = asyncHttpClient.executeAsyncHttpRequest(request)

        return parseJsonResponse(debugTokenUri, response, debugTokenResponseReader)
    }

    suspend fun fetchUsersOwnProfile(accessToken: String): UserIdentity {
        val uri = URI("https://graph.facebook.com/me")
        val response = asyncHttpClient.executeAsyncHttpRequest(HttpGet(uri.withParameters(
                "access_token" to accessToken,
                "fields" to "id,name,picture"
        )))
        return parseJsonResponse(uri, response, userIdentityReader)
    }

    suspend fun fetchUserProfile(userId: String): UserIdentity {
        val uri = URI("https://graph.facebook.com/$userId")
        val response = asyncHttpClient.executeAsyncHttpRequest(HttpGet(uri.withParameters(
                "access_token" to "${config.id}|${config.secret}",
                "fields" to "id,name,picture"
        )))
        return parseJsonResponse(uri, response, userIdentityReader)
    }

    private fun URI.withParameters(vararg parameters: Pair<String, String>): URI =
            URIBuilder(this).setParameters(parameters.map { BasicNameValuePair(it.first, it.second) }).build()
}
