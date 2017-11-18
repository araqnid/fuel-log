package org.araqnid.fuellog

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.message.BasicNameValuePair
import org.apache.http.nio.client.HttpAsyncClient
import org.araqnid.fuellog.events.FacebookProfileData
import org.araqnid.fuellog.events.GoogleProfileData
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.BiConsumer
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.BadRequestException
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.Context

@Singleton
@Path("/user/identity")
class IdentityResources @Inject constructor(val clock: Clock, val asyncHttpClient: HttpAsyncClient, val jettyService: JettyService, val userRepository: UserRepository,
                                            val facebookClientConfig: FacebookClientConfig, val googleClientConfig: GoogleClientConfig) {
    private val logger = LoggerFactory.getLogger(IdentityResources::class.java)

    @GET
    @Produces("application/json")
    @PermitAll
    fun currentUser(@Context servletRequest: HttpServletRequest): CurrentUser {
        val userId = servletRequest.maybeSession?.userId
        val userInfo = if (userId != null) UserInfo.from(userRepository[userId]) else null
        return CurrentUser(userInfo)
    }

    @DELETE
    @PermitAll
    fun unsetCurrentUser(@Context servletRequest: HttpServletRequest) {
        servletRequest.maybeSession?.invalidate()
    }

    @POST
    @Path("test")
    @Produces("application/json")
    @PermitAll
    fun associateTestUser(@FormParam("identifier") identifier: String, @FormParam("name") name: String,
                          @Context servletRequest: HttpServletRequest): UserInfo {
        val user = associateUser(servletRequest, URI.create("https://fuel.araqnid.org/_api/user/identity/test/$identifier"))
        user.name = name
        userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))
        return UserInfo.from(user)
    }

    @POST
    @Path("facebook")
    @Produces("application/json")
    @PermitAll
    fun associateFacebookUser(@FormParam("id") identifier: String, @FormParam("name") name: String, @FormParam("picture") picture: URI,
                              @FormParam("token") token: String,
                              @Context servletRequest: HttpServletRequest, @Suspended asyncResponse: AsyncResponse) {
        FacebookClient(facebookClientConfig, asyncHttpClient)
                .validateUserAccessToken(token)
                .thenApply { parsed ->
                    if (parsed.userId != identifier)
                        throw BadRequestException("Different user ID in request compared to access token")

                    val user = associateUser(servletRequest, URI.create("https://fuel.araqnid.org/_api/user/identity/facebook/$identifier"))
                    user.name = name
                    user.facebookProfileData = FacebookProfileData(picture)
                    userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))
                    UserInfo.from(user)
                }
                .exceptionally { ex ->
                    logger.warn("Failed to verify Facebook token", ex)
                    throw BadRequestException("Failed to verify Facebook token")
                }
                .thenRespondTo(asyncResponse)
    }

    @POST
    @Path("google")
    @Consumes("text/plain")
    @Produces("application/json")
    @PermitAll
    fun associateGoogleUserAsync(idToken: String, @Context servletRequest: HttpServletRequest, @Suspended asyncResponse: AsyncResponse) {
        val tokenInfoUri = URI.create("https://www.googleapis.com/oauth2/v3/tokeninfo")

        val request = HttpPost(tokenInfoUri).apply {
            entity = UrlEncodedFormEntity(listOf(BasicNameValuePair("id_token", idToken)))
        }

        executeAsyncHttpRequest(request).thenApply { response ->
            if (response.statusLine.statusCode != HttpStatus.SC_OK)
                throw BadRequestException("$tokenInfoUri: ${response.statusLine}")
            val tokenInfo = objectMapperForGoogleEndpoint.readerFor(GoogleTokenInfo::class.java)
                    .readValue<GoogleTokenInfo>(response.entity.content)!!
            if (tokenInfo.clientId != googleClientConfig.id)
                throw BadRequestException("Token is not for our client ID: $tokenInfo")
            if (tokenInfo.expiresAt < Instant.now(clock))
                throw BadRequestException("Token already expired: $tokenInfo")
            if (tokenInfo.issuedBy != "accounts.google.com" && tokenInfo.issuedBy != "https://accounts.google.com")
                throw BadRequestException("Token issuer is unrecognised: $tokenInfo")

            val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/google/${tokenInfo.userId}")!!

            val user = associateUser(servletRequest, externalId)
            user.name = tokenInfo.name
            user.googleProfileData = tokenInfo.toProfileData()
            userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))

            UserInfo.from(user)
        }.thenRespondTo(asyncResponse)
    }

    private fun associateUser(servletRequest: HttpServletRequest, externalId: URI): UserRecord {
        val metadata = RequestMetadata.fromServletRequest(servletRequest)

        val user = userRepository.findOrCreateUserByExternalId(externalId, metadata)

        with(servletRequest.maybeSession) {
            if (this != null && userId != user.userId) invalidate()
        }
        with(servletRequest.session) {
            // maybe new session
            if (userId == null) userId = user.userId
        }

        return user
    }

    data class CurrentUser(val userInfo: UserInfo?)

    data class UserInfo(val userId: UUID, val name: String?, val realm: UserRecord.Realm, val picture: URI?) {
        companion object {
            fun from(user: UserRecord): UserInfo = UserInfo(user.userId, user.name, user.realm, user.picture)
        }
    }

    private val objectMapperForGoogleEndpoint = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    data class GoogleTokenInfo(val name: String,
                               @JsonProperty("iat") val issuedAt: Instant,
                               @JsonProperty("exp") val expiresAt: Instant,
                               @JsonProperty("aud") val clientId: String,
                               @JsonProperty("sub") val userId: String,
                               @JsonProperty("iss") val issuedBy: String,
                               val givenName: String?,
                               val familyName: String?,
                               val picture: URI?,
                               private val others: MutableMap<String, Any> = LinkedHashMap<String, Any>()) {
        @JsonAnySetter
        fun addOther(name: String, value: Any) {
            others[name] = value
        }

        fun toProfileData(): GoogleProfileData = GoogleProfileData(givenName, familyName, picture)
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

    private fun executeAsyncHttpRequest(request: HttpUriRequest): CompletionStage<HttpResponse> {
        return CompletableFuture<HttpResponse>().apply {
            asyncHttpClient.execute(request, apacheCallback(this))
        }.withContextData()
    }

    private fun <T> CompletionStage<T>.thenRespondTo(asyncResponse: AsyncResponse): CompletionStage<T> {
        return whenCompleteAsync(BiConsumer { result, ex ->
            when {
                result != null -> asyncResponse.resume(result)
                ex != null -> asyncResponse.resume(ex)
                else -> throw IllegalStateException()
            }
        }, jettyService.server.threadPool)
    }
}
