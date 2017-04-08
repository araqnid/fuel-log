package org.araqnid.fuellog

import org.eclipse.jetty.security.Authenticator
import org.eclipse.jetty.security.RoleInfo
import org.eclipse.jetty.security.SecurityHandler
import org.eclipse.jetty.security.authentication.SessionAuthentication
import org.eclipse.jetty.server.Authentication
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.UserIdentity
import org.slf4j.LoggerFactory
import java.security.Principal
import java.util.UUID
import javax.annotation.security.PermitAll
import javax.security.auth.Subject
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.ext.Provider

@Provider
class LocalUserSecurityFeature : DynamicFeature {
    private val logger = LoggerFactory.getLogger(LocalUserSecurityFeature::class.java)

    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        context.register(Filter(resourceInfo.hasAnnotation<PermitAll>()))
    }

    internal inner class Filter(val permitAll: Boolean) : ContainerRequestFilter {
        override fun filter(requestContext: ContainerRequestContext) {
            val securityContext: SecurityContext = requestContext.securityContext
            logger.debug("filter: user=${securityContext.userPrincipal} permitAll=$permitAll for ${requestContext.uriInfo.requestUri}")
            if (securityContext.userPrincipal == null && !permitAll)
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Authentication required").build())
        }
    }

    private inline fun <reified T : Annotation> ResourceInfo.hasAnnotation(): Boolean {
        return resourceMethod.isAnnotationPresent(T::class.java)
            || resourceClass.isAnnotationPresent(T::class.java)
    }
}

data class LocalUser(val id: UUID) : Principal {
    override fun getName() = id.toString() // this is what shows up in request logs etc
}

class LocalUserSecurityHandler : SecurityHandler() {
    val methodName = "LOCAL"

    private val logger = LoggerFactory.getLogger(LocalUserSecurityHandler::class.java)

    init {
        authenticator = object : Authenticator {
            private val logger = LoggerFactory.getLogger(LocalUserSecurityHandler::class.qualifiedName + ".Authenticator")

            override fun prepareRequest(request: ServletRequest) {
                logger.debug("prepareRequest(<req>)")
            }

            override fun validateRequest(request: ServletRequest, response: ServletResponse, mandatory: Boolean): Authentication {
                logger.debug("validateRequest(<req>, <resp>, mandatory: $mandatory)")
                val userId = (request as HttpServletRequest).maybeSession?.userId
                return when (userId) {
                    null -> Authentication.UNAUTHENTICATED
                    else -> SessionAuthentication(methodName, LocalUserIdentity(userId), userId)
                }
            }

            override fun secureResponse(request: ServletRequest, response: ServletResponse, mandatory: Boolean, validatedUser: Authentication.User?): Boolean {
                logger.debug("secureResponse(<req>, <resp>, mandatory: $mandatory, validatedUser: $validatedUser)")
                return true
            }

            override fun setConfiguration(configuration: Authenticator.AuthConfiguration) {
                logger.debug("setConfiguration($configuration)")
            }

            override fun getAuthMethod() = methodName
        }
    }

    data class LocalUserIdentity(val userId: UUID) : UserIdentity {
        val user = LocalUser(userId)
        val javaxSubject = Subject().apply {
            principals.add(user)
            setReadOnly()
        }

        override fun getSubject() = javaxSubject
        override fun isUserInRole(role: String?, scope: UserIdentity.Scope?) = false
        override fun getUserPrincipal() = user
    }

    override fun isAuthMandatory(baseRequest: Request, baseResponse: org.eclipse.jetty.server.Response, constraintInfo: Any): Boolean {
        logger.debug("isAuthMandatory(<req>, <resp>, constraintInfo: $constraintInfo)")
        return true
    }

    override fun checkUserDataPermissions(pathInContext: String, request: Request, response: org.eclipse.jetty.server.Response, constraintInfo: RoleInfo): Boolean {
        logger.debug("checkUserDataPermissions(pathInContext: $pathInContext, <req>, <resp>, constraintInfo: $constraintInfo)")
        return true
    }

    override fun prepareConstraintInfo(pathInContext: String, request: Request): RoleInfo {
        logger.debug("prepareConstraintInfo(pathInContext: $pathInContext, <req>)")
        return RoleInfo()
    }

    override fun checkWebResourcePermissions(pathInContext: String, request: Request, response: org.eclipse.jetty.server.Response, constraintInfo: Any, userIdentity: UserIdentity): Boolean {
        logger.debug("checkWebResourcePermissions(pathInContext: $pathInContext, <req>, <resp>, constraintInfo: $constraintInfo, userIdentity: $userIdentity)")
        return true // authorisation handled by app
    }
}
