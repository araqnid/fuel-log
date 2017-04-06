package org.araqnid.fuellog

import org.araqnid.appstatus.AppVersion
import org.araqnid.appstatus.Readiness
import org.araqnid.appstatus.StatusComponent
import org.araqnid.appstatus.StatusPage
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/info")
@PermitAll
class InfoResources @Inject constructor(val appVersion: AppVersion,
                                        val statusComponents: @JvmSuppressWildcards Collection<StatusComponent>,
                                        val readinessListener: ReadinessListener) {
    @Path("readiness")
    @GET
    @Produces("text/plain; charset=utf-8")
    fun readiness(): Readiness = readinessListener.readiness

    @Path("version")
    @GET
    @Produces("application/json")
    fun version() = appVersion

    @Path("version")
    @GET
    @Produces("text/plain; charset=utf-8")
    fun versionText() = appVersion.version ?: ""

    @Path("status")
    @GET
    @Produces("application/json")
    fun statusPage(): StatusPage = StatusPage.build(statusComponents)
}
