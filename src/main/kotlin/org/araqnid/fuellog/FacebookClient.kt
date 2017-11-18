package org.araqnid.fuellog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.utils.URIBuilder
import org.apache.http.concurrent.FutureCallback
import org.apache.http.message.BasicNameValuePair
import org.apache.http.nio.client.HttpAsyncClient
import java.lang.Exception
import java.net.URI
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class FacebookClient(val config: FacebookClientConfig, private val asyncHttpClient: HttpAsyncClient) {
    val debugTokenUri = URI.create("https://graph.facebook.com/debug_token")
    val oauthAccessTokenUri = URI.create("https://graph.facebook.com/oauth/access_token")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AccessTokenResponse(val accessToken: String, val tokenType: String)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DebugTokenResponse(val userId: String, val type: String, val appId: String, val application: String,
                                  val expires_at: Instant, val isValid: Boolean, val scopes: Set<String>)

    private val objectMapperForFacebookEndpoint = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    fun fetchFacebookAppToken(): CompletionStage<String> {

        val request = HttpGet(URIBuilder(oauthAccessTokenUri).setParameters(listOf(
                BasicNameValuePair("client_id", config.id),
                BasicNameValuePair("client_secret", config.secret),
                BasicNameValuePair("grant_type", "client_credentials")
        )).build())
        return executeAsyncHttpRequest(request).thenApply { response ->
            if (response.statusLine.statusCode != HttpStatus.SC_OK) throw RuntimeException("Failed to fetch app access token from Facebook: ${response.statusLine}")
            objectMapperForFacebookEndpoint
                    .readerFor(AccessTokenResponse::class.java)
                    .readValue<AccessTokenResponse>(response.entity.content)
                    .accessToken
        }
    }

    fun validateUserAccessToken(token: String): CompletionStage<DebugTokenResponse> {
        return fetchFacebookAppToken().thenCompose { appToken ->
            val request = HttpGet(URIBuilder(debugTokenUri)
                    .setParameters(listOf(
                            BasicNameValuePair("input_token", token),
                            BasicNameValuePair("access_token", appToken) // or "${facebookClientConfig.id}|${facebookClientConfig.secret}"
                    ))
                    .build())

            executeAsyncHttpRequest(request)
        }.thenApply { response ->
            if (response.statusLine.statusCode != HttpStatus.SC_OK) throw RuntimeException("Failed to validate Facebook access token: ${response.statusLine}")

            objectMapperForFacebookEndpoint
                    .readerFor(DebugTokenResponse::class.java)
                    .withRootName("data")
                    .readValue<DebugTokenResponse>(response.entity.content)
        }
    }

    private fun executeAsyncHttpRequest(request: HttpUriRequest): CompletionStage<HttpResponse> {
        return CompletableFuture<HttpResponse>().apply {
            asyncHttpClient.execute(request, apacheCallback(this))
        }.withContextData()
    }

    private fun <T> apacheCallback(target: CompletableFuture<T>) = object : FutureCallback<T> {
        override fun completed(result: T) {
            target.complete(result)
        }

        override fun failed(ex: Exception) {
            target.completeExceptionally(ex)
        }

        override fun cancelled() {
            target.cancel(false)
        }
    }
}