package org.araqnid.fuellog

import org.araqnid.eventstore.EventStreamWriter
import org.araqnid.eventstore.NoSuchStreamException
import org.araqnid.eventstore.StreamId
import org.araqnid.eventstore.emptyStreamEventNumber
import org.araqnid.fuellog.events.Coordinates
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.core.UriInfo

@Path("/fuel")
class FuelResources @Inject constructor(val streamWriter: EventStreamWriter, val fuelRepository: FuelRepository, val clock: Clock) {
    @GET
    @Path("/{purchaseId}")
    @Produces("application/json")
    fun getPurchase(@PathParam("purchaseId") purchaseId: String): FuelRecord {
        return try {
            fuelRepository[UUID.fromString(purchaseId)]
        } catch (e: NoSuchStreamException) {
            throw NotFoundException(e)
        }
    }

    @GET
    @Produces("application/json")
    fun findPurchasesForUser(@Context securityContext: SecurityContext): Collection<FuelRecord> {
        val user = securityContext.userPrincipal as LocalUser
        return fuelRepository.byUserId(user.id)
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    fun newFuelPurchase(fuelPurchased: NewFuelPurchase, @Context servletRequest: HttpServletRequest, @Context uriInfo: UriInfo, @Context securityContext: SecurityContext): Response {
        val purchaseId = UUID.randomUUID()!!
        val userId = (securityContext.userPrincipal as LocalUser).id
        val streamId = StreamId("fuel", purchaseId.toString())
        val event = FuelPurchased(
                timestamp = Instant.now(clock),
                userId = userId,
                fuelVolume = fuelPurchased.fuelVolume,
                cost = fuelPurchased.cost,
                odometer = fuelPurchased.odometer,
                fullFill = fuelPurchased.fullFill,
                location = fuelPurchased.location,
                geoLocation = fuelPurchased.geoLocation)
        streamWriter.write(streamId, emptyStreamEventNumber, listOf(EventCodecs.encode(event, RequestMetadata.fromServletRequest(servletRequest))))
        return Response.created(uriInfo.requestUriBuilder.path(purchaseId.toString()).build()).build()
    }

    data class NewFuelPurchase(val fuelVolume: Double, val cost: MonetaryAmount, val location: String, val odometer: Double, val fullFill: Boolean,
                               val geoLocation: Coordinates?)
}
