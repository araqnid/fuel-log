package org.araqnid.fuellog

import org.araqnid.appstatus.AppVersion
import java.util.UUID
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.SecurityContext

@Path("/")
@PermitAll
class RootResource @Inject constructor(val appVersion: AppVersion, val userRepository: UserRepository) {
    @GET
    @Produces("application/json")
    fun root(@Context securityContext: SecurityContext): ApiInfo {
        val localUser = securityContext.userPrincipal as? LocalUser
        val user = localUser?.let { userRepository.findUser(it.id) }
        return ApiInfo(version = appVersion.version, userInfo = user?.let { IdentityResources.UserInfo.from(it) })
    }

    data class ApiInfo(val version: String?, val userInfo: IdentityResources.UserInfo?)
}
