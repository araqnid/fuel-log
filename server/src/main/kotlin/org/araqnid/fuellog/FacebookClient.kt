@file:UseSerializers(EpochSecondsSerializer::class, URISerializer::class)

package org.araqnid.fuellog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
import java.net.URI
import java.net.http.HttpClient
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

class FacebookClient @Inject constructor(
    @Named("FacebookGraphUri") private val graphUri: URI,
    private val config: FacebookClientConfig,
    private val httpClient: HttpClient
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
        return httpClient.getJson(AccessTokenResponse.serializer()) {
            uri(
                graphUri.resolve("oauth/access_token").withParameters(
                    "client_id" to config.id,
                    "client_secret" to config.secret,
                    "grant_type" to "client_credentials"
                )
            )
        }.accessToken
    }

    suspend fun validateUserAccessToken(token: String): DebugTokenResponse {
        return httpClient.getJson(DebugTokenResponse.serializer()) {
            uri(
                graphUri.resolve("debug_token").withParameters(
                    "input_token" to token,
                    "access_token" to "${config.id}|${config.secret}"
                )
            )
        }
    }

    suspend fun fetchUsersOwnProfile(accessToken: String): UserIdentity {
        return httpClient.getJson(UserIdentity.serializer()) {
            uri(
                graphUri.resolve("me").withParameters(
                    "access_token" to accessToken,
                    "fields" to "id,name,picture"
                )
            )
        }
    }

    suspend fun fetchUserProfile(userId: String): UserIdentity {
        return httpClient.getJson(UserIdentity.serializer()) {
            uri(
                graphUri.resolve(userId).withParameters(
                    "access_token" to "${config.id}|${config.secret}",
                    "fields" to "id,name,picture"
                )
            )
        }
    }

    private fun URI.withParameters(vararg parameters: Pair<String, String>): URI =
        URIBuilder(this).setParameters(parameters.map { BasicNameValuePair(it.first, it.second) }).build()
}
