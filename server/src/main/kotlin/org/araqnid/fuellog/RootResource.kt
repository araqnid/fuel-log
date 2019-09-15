package org.araqnid.fuellog

import org.araqnid.appstatus.AppVersion
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.SecurityContext

@Path("/")
@PermitAll
class RootResource @Inject constructor(
        val appVersion: AppVersion,
        val userRepository: UserRepository,
        @Named("GOOGLE_MAPS_API_KEY") val googleMapsApiKey: String?
) {
    @GET
    @Produces("application/json")
    fun root(@Context securityContext: SecurityContext): ApiInfo {
        val localUser = securityContext.userPrincipal as? LocalUser
        val user = localUser?.let { userRepository.findUser(it.id) }
        return ApiInfo(
                version = appVersion.version,
                userInfo = user?.let { IdentityResources.UserInfo.from(it) },
                googleMapsApiKey = googleMapsApiKey
        )
    }

    data class ApiInfo(val version: String?, val userInfo: IdentityResources.UserInfo?, val googleMapsApiKey: String?)
}
