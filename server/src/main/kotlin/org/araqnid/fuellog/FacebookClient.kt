@file:UseSerializers(EpochSecondsSerializer::class, URISerializer::class)

package org.araqnid.fuellog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.nio.client.HttpAsyncClient
import java.net.URI
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

class FacebookClient @Inject constructor(
    @Named("FacebookDebugTokenUri") private val debugTokenUri: URI,
    @Named("FacebookOauthAccessTokenUri") private val oauthAccessTokenUri: URI,
    private val config: FacebookClientConfig,
    private val asyncHttpClient: HttpAsyncClient
) {
    @Serializable
    data class AccessTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("token_type") val tokenType: String
    )

    @Serializable
    data class DebugTokenResponse(
        @SerialName("user_id") val userId: String,
        val type: String,
        @SerialName("app_id") val appId: String,
        val application: String,
        @SerialName("expires_at") val expiresAt: Instant,
        @SerialName("is_valid") val isValid: Boolean,
        val scopes: Set<String>
    )

    @Serializable
    data class UserIdentity(val name: String, val id: String, val picture: Picture)

    @Serializable
    data class Picture(val data: PictureData)

    @Serializable
    data class PictureData(
        val width: Int,
        val height: Int,
        val url: URI,
        @SerialName("is_silhouette") val silhouette: Boolean
    )

    suspend fun fetchFacebookAppToken(): String {
        val request = HttpGet(
            oauthAccessTokenUri.withParameters(
                "client_id" to config.id,
                "client_secret" to config.secret,
                "grant_type" to "client_credentials"
            )
        )

        val response = asyncHttpClient.executeAsyncHttpRequest(request)

        return parseJsonResponse(
            oauthAccessTokenUri,
            response,
            AccessTokenResponse.serializer(),
        ).accessToken
    }

    suspend fun validateUserAccessToken(token: String): DebugTokenResponse {
        val request = HttpGet(
            debugTokenUri.withParameters(
                "input_token" to token,
                "access_token" to "${config.id}|${config.secret}"
            )
        )

        val response = asyncHttpClient.executeAsyncHttpRequest(request)

        return parseJsonResponse(debugTokenUri, response, DebugTokenResponse.serializer())
    }

    suspend fun fetchUsersOwnProfile(accessToken: String): UserIdentity {
        val uri = URI("https://graph.facebook.com/me")
        val response = asyncHttpClient.executeAsyncHttpRequest(
            HttpGet(
                uri.withParameters(
                    "access_token" to accessToken,
                    "fields" to "id,name,picture"
                )
            )
        )
        return parseJsonResponse(uri, response, UserIdentity.serializer())
    }

    suspend fun fetchUserProfile(userId: String): UserIdentity {
        val uri = URI("https://graph.facebook.com/$userId")
        val response = asyncHttpClient.executeAsyncHttpRequest(
            HttpGet(
                uri.withParameters(
                    "access_token" to "${config.id}|${config.secret}",
                    "fields" to "id,name,picture"
                )
            )
        )
        return parseJsonResponse(uri, response, UserIdentity.serializer())
    }

    private fun URI.withParameters(vararg parameters: Pair<String, String>): URI =
        URIBuilder(this).setParameters(parameters.map { BasicNameValuePair(it.first, it.second) }).build()
}
