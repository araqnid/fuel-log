package org.araqnid.fuellog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicNameValuePair
import org.apache.http.nio.client.HttpAsyncClient
import java.net.URI
import java.time.Instant
import java.util.concurrent.CompletionStage
import javax.ws.rs.BadRequestException

class FacebookClient(val config: FacebookClientConfig, private val asyncHttpClient: HttpAsyncClient) {
    val debugTokenUri = URI.create("https://graph.facebook.com/debug_token")
    val oauthAccessTokenUri = URI.create("https://graph.facebook.com/oauth/access_token")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AccessTokenResponse(val accessToken: String, val tokenType: String)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DebugTokenResponse(val userId: String, val type: String, val appId: String, val application: String,
                                  val expiresAt: Instant, val isValid: Boolean, val scopes: Set<String>)
    data class UserIdentity(val name: String, val id: String, val picture: Picture)
    data class Picture(val data: PictureData)
    data class PictureData(val width: Int, val height: Int, val url: URI, @JsonProperty("is_silhouette") val silhouette: Boolean)

    private val objectMapperForFacebookEndpoint = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    fun fetchFacebookAppToken(): CompletionStage<String> {
        val request = HttpGet(URIBuilder(oauthAccessTokenUri)
                .setParameters(listOf(
                        BasicNameValuePair("client_id", config.id),
                        BasicNameValuePair("client_secret", config.secret),
                        BasicNameValuePair("grant_type", "client_credentials")
                )).build())

        return asyncHttpClient.executeAsyncHttpRequest(request).thenApply { response ->
            if (response.statusLine.statusCode != HttpStatus.SC_OK)
                throw BadRequestException("$oauthAccessTokenUri: ${response.statusLine}")

            objectMapperForFacebookEndpoint
                    .readerFor(AccessTokenResponse::class.java)
                    .readValue<AccessTokenResponse>(response.entity.content)
                    .accessToken
        }
    }

    fun validateUserAccessToken(token: String): CompletionStage<DebugTokenResponse> {
        val request = HttpGet(URIBuilder(debugTokenUri)
                .setParameters(listOf(
                        BasicNameValuePair("input_token", token),
                        BasicNameValuePair("access_token", "${config.id}|${config.secret}")
                ))
                .build())

        return asyncHttpClient.executeAsyncHttpRequest(request)
                .thenApply { response ->
                    if (response.statusLine.statusCode != HttpStatus.SC_OK)
                        throw BadRequestException("$debugTokenUri: ${response.statusLine}")

                    objectMapperForFacebookEndpoint
                            .readerFor(DebugTokenResponse::class.java)
                            .withRootName("data")
                            .readValue<DebugTokenResponse>(response.entity.content)
                }
    }

    fun fetchUsersOwnProfile(accessToken: String): CompletionStage<UserIdentity> {
        val uri = URI.create("https://graph.facebook.com/me")
        return asyncHttpClient.executeAsyncHttpRequest(HttpGet(URIBuilder(uri).setParameters(
                BasicNameValuePair("access_token", accessToken),
                BasicNameValuePair("fields", "id,name,picture")
        ).build())).parseJsonResponse(uri)
    }

    fun fetchUserProfile(userId: String): CompletionStage<UserIdentity> {
        val uri = URI.create("https://graph.facebook.com/$userId")
        return asyncHttpClient.executeAsyncHttpRequest(HttpGet(URIBuilder(uri).setParameters(
                BasicNameValuePair("access_token", "${config.id}|${config.secret}"),
                BasicNameValuePair("fields", "id,name,picture")
        ).build())).parseJsonResponse(uri)
    }

    val permittedMimeTypes = setOf("text/javacsript", "application/json")

    private inline fun <reified T> CompletionStage<HttpResponse>.parseJsonResponse(uri: URI): CompletionStage<T> {
        return thenApply { response ->
            if (response.statusLine.statusCode != HttpStatus.SC_OK)
                throw BadRequestException("$uri: ${response.statusLine}")

            val contentType = ContentType.get(response.entity)
            if (permittedMimeTypes.contains(contentType.mimeType.toLowerCase()))
                throw BadRequestException("$uri: unhandled content-type: $contentType")

            objectMapperForFacebookEndpoint
                    .readerFor(T::class.java)
                    .readValue<T>(response.entity.content)
        }
    }
}