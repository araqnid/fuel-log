package org.araqnid.fuellog

import kotlinx.coroutines.experimental.future.future
import org.apache.http.nio.client.HttpAsyncClient
import org.araqnid.fuellog.events.FacebookProfileData
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Clock
import java.util.*
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
    @Consumes("text/plain")
    @Produces("application/json")
    @PermitAll
    fun associateFacebookUser(token: String,
                              @Context servletRequest: HttpServletRequest, @Suspended asyncResponse: AsyncResponse) {
        respondTo(asyncResponse) {
            try {
                val parsed = FacebookClient(facebookClientConfig, asyncHttpClient).fetchUserProfile(token)
                val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/facebook/${parsed.id}")!!

                val user = associateUser(servletRequest, externalId)
                user.name = parsed.name
                user.facebookProfileData = FacebookProfileData(parsed.picture.data.url)
                userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))

                UserInfo.from(user)
            } catch (ex: Exception) {
                logger.warn("Failed to verify Facebook token", ex)
                throw BadRequestException("Failed to verify Facebook token")
            }
        }
    }

    @POST
    @Path("google")
    @Consumes("text/plain")
    @Produces("application/json")
    @PermitAll
    fun associateGoogleUserAsync(idToken: String, @Context servletRequest: HttpServletRequest, @Suspended asyncResponse: AsyncResponse) {
        respondTo(asyncResponse) {
            try {
                val tokenInfo = GoogleClient(googleClientConfig, asyncHttpClient, clock).validateToken(idToken)
                val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/google/${tokenInfo.userId}")!!

                val user = associateUser(servletRequest, externalId)
                user.name = tokenInfo.name
                user.googleProfileData = tokenInfo.toProfileData()
                userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))

                UserInfo.from(user)
            } catch (ex: Exception) {
                logger.warn("Failed to verify Google token", ex)
                throw BadRequestException("Failed to verify Google token")
            }
        }
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

    private fun <T> respondTo(asyncResponse: AsyncResponse, block: suspend () -> T) {
        future(ResteasyAsync()) {
            block()
        }.whenCompleteAsync(BiConsumer { result, ex ->
            when {
                ex != null -> asyncResponse.resume(ex)
                else -> asyncResponse.resume(result)
            }
        }, jettyService.server.threadPool)
    }
}
