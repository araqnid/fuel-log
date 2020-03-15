package org.araqnid.fuellog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.araqnid.fuellog.events.GoogleProfileData
import java.net.URI
import java.net.http.HttpClient
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.ws.rs.BadRequestException

class GoogleClient @Inject constructor(
    private val config: GoogleClientConfig,
    private val httpClient: HttpClient,
    private val clock: Clock
) {
    private val tokenInfoUri = URI("https://www.googleapis.com/oauth2/v3/tokeninfo")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TokenInfo(
        val name: String,
        @JsonProperty("iat") val issuedAt: Instant,
        @JsonProperty("exp") val expiresAt: Instant,
        @JsonProperty("aud") val clientId: String,
        @JsonProperty("sub") val userId: String,
        @JsonProperty("iss") val issuedBy: String,
        val givenName: String?,
                         val familyName: String?,
                         val picture: URI?) {
        fun toProfileData(): GoogleProfileData = GoogleProfileData(givenName, familyName, picture)
    }

    private val objectMapperForGoogleEndpoint = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    private val tokenInfoReader = objectMapperForGoogleEndpoint.readerFor(TokenInfo::class.java)

    suspend fun validateToken(idToken: String): TokenInfo {
        val tokenInfo = httpClient.getJson<TokenInfo>(tokenInfoReader) {
            POSTFormData("id_token" to idToken)
            uri(tokenInfoUri)
        }

        if (tokenInfo.clientId != config.id)
            throw BadRequestException("Token is not for our client ID: $tokenInfo")
        if (tokenInfo.expiresAt < Instant.now(clock))
            throw BadRequestException("Token already expired: $tokenInfo")
        if (tokenInfo.issuedBy != "accounts.google.com" && tokenInfo.issuedBy != "https://accounts.google.com")
            throw BadRequestException("Token issuer is unrecognised: $tokenInfo")

        return tokenInfo
    }
}
