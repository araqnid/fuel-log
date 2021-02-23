@file:UseSerializers(EpochSecondsSerializer::class, URISerializer::class)

package org.araqnid.fuellog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.araqnid.fuellog.events.GoogleProfileData
import java.net.URI
import java.net.http.HttpClient
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.BadRequestException

class GoogleClient @Inject constructor(
    @Named("GoogleTokenInfo") private val tokenInfoUri: URI,
    private val config: GoogleClientConfig,
    private val httpClient: HttpClient,
    private val clock: Clock
) {
    @Serializable
    data class TokenInfo(
        val name: String,
        @SerialName("iat") val issuedAt: Instant,
        @SerialName("exp") val expiresAt: Instant,
        @SerialName("aud") val clientId: String,
        @SerialName("sub") val userId: String,
        @SerialName("iss") val issuedBy: String,
        val givenName: String? = null,
        val familyName: String? = null,
        val picture: URI? = null
    ) {
        fun toProfileData(): GoogleProfileData = GoogleProfileData(givenName, familyName, picture)
    }

    suspend fun validateToken(idToken: String): TokenInfo {
        return httpClient.getJson(TokenInfo.serializer()) {
            POSTFormData("id_token" to idToken)
            uri(tokenInfoUri)
        }.also { tokenInfo ->
            if (tokenInfo.clientId != config.id)
                throw BadRequestException("Token is not for our client ID: $tokenInfo")
            if (tokenInfo.expiresAt < Instant.now(clock))
                throw BadRequestException("Token already expired: $tokenInfo")
            if (tokenInfo.issuedBy != "accounts.google.com" && tokenInfo.issuedBy != "https://accounts.google.com")
                throw BadRequestException("Token issuer is unrecognised: $tokenInfo")
        }
    }
}
