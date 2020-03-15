package org.araqnid.fuellog

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.future.future
import org.araqnid.fuellog.events.FacebookProfileData
import org.araqnid.kotlin.coroutines.resteasy.ResteasyContext
import java.net.URI
import java.time.Clock
import java.util.UUID
import java.util.concurrent.CompletionStage
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context

@Singleton
@Path("/user/identity")
class IdentityResources @Inject constructor(
    clock: Clock,
    private val googleClient: GoogleClient,
    private val facebookClient: FacebookClient,
    private val jettyService: JettyService,
    private val userRepository: UserRepository
) {
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
                          @Context servletRequest: HttpServletRequest): CompletionStage<UserInfo> {
        return jettyService.future(CoroutineName("associateTestUser") + ResteasyContext()) {
            val user = associateUser(servletRequest,
                    URI.create("https://fuel.araqnid.org/_api/user/identity/test/$identifier"))
            user.name = name
            userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))
            UserInfo.from(user)
        }
    }

    @POST
    @Path("facebook")
    @Consumes("text/plain")
    @Produces("application/json")
    @PermitAll
    fun associateFacebookUser(token: String,
                              @Context servletRequest: HttpServletRequest): CompletionStage<UserInfo> {
        return jettyService.future(CoroutineName("associateFacebookUser") + ResteasyContext()) {
            val parsed = facebookClient.fetchUsersOwnProfile(token)
            val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/facebook/${parsed.id}")!!

            val user = associateUser(servletRequest, externalId)
            user.name = parsed.name
            user.facebookProfileData = FacebookProfileData(parsed.picture.data.url)
            userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))

            UserInfo.from(user)
        }
    }

    @POST
    @Path("google")
    @Consumes("text/plain")
    @Produces("application/json")
    @PermitAll
    fun associateGoogleUser(idToken: String, @Context servletRequest: HttpServletRequest): CompletionStage<UserInfo> {
        return jettyService.future(CoroutineName("associateGoogleUser") + ResteasyContext()) {
            val tokenInfo = googleClient.validateToken(idToken)
            val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/google/${tokenInfo.userId}")!!

            val user = associateUser(servletRequest, externalId)
            user.name = tokenInfo.name
            user.googleProfileData = tokenInfo.toProfileData()
            userRepository.save(user, RequestMetadata.fromServletRequest(servletRequest))

            UserInfo.from(user)
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
}
