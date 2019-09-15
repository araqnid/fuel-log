package org.araqnid.fuellog

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.cast
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.araqnid.eventstore.EventCategoryReader
import org.araqnid.eventstore.EventStreamWriter
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.Coordinates
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.EventMetadata
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import org.araqnid.fuellog.hamkrest.containsInOrder
import org.araqnid.hamkrest.json.jsonObject
import org.junit.Test
import java.time.Instant
import java.util.UUID

class FuelResourcesIntegrationTest : IntegrationTest() {
    val emptyMetadata = object : EventMetadata {}

    @Test fun get_fuel_purchase() {
        val purchaseId = UUID.randomUUID()
        loginAsNewUser()

        server.instance<EventStreamWriter>().write(StreamId("fuel", purchaseId.toString()), listOf(
                EventCodecs.encode(FuelPurchased(
                        Instant.now(server.clock),
                        currentUser.userId,
                        50.0,
                        MonetaryAmount("GBP", 100.0),
                        110000.0,
                        true,
                        "Some place",
                        geoLocation = Coordinates(51.2, 0.02)
                ), emptyMetadata)
        ))

        val response = execute(HttpGet("/_api/fuel/$purchaseId"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        val fuelRecord = response.readJson<FuelRecord>()
        assertThat(fuelRecord.userId, equalTo(currentUser.userId))
        assertThat(fuelRecord.fuelPurchaseId, equalTo(purchaseId))
        assertThat(fuelRecord.fuelVolume, equalTo(50.0))
        assertThat(fuelRecord.geoLocation, equalTo(Coordinates(51.2, 0.02)))
    }

    @Test fun post_new_fuel_purchase() {
        val user = loginAsNewUser()

        val newFuelPurchase = FuelResources.NewFuelPurchase(
                fuelVolume = 45.67,
                cost = MonetaryAmount("GBP", 99.87),
                odometer = 111234.0,
                fullFill = true,
                location = "Somewhere",
                geoLocation = Coordinates(51.2, 0.02)
        )

        val response = execute(HttpPost("/_api/fuel").apply { entity = JacksonEntity(newFuelPurchase) })
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_CREATED))

        val fuelPurchasedEvent = FuelPurchased(
                timestamp = server.clock.instant(),
                userId = currentUser.userId,
                fuelVolume = 45.67,
                cost = MonetaryAmount("GBP", 99.87),
                odometer = 111234.0,
                fullFill = true,
                location = "Somewhere",
                geoLocation = Coordinates(51.2, 0.02))

        assertThat(serverEvents(), containsInOrder(
                has(DecodedEvent::category, equalTo("fuel"))
                        and has(DecodedEvent::data, cast(equalTo(fuelPurchasedEvent)))
                        and has(DecodedEvent::metadata, cast(jsonObject().withProperty("client_ip", "127.0.0.1").withAnyOtherProperties()))
        ))

        val streamId = server.instance<EventCategoryReader>().readCategoryForwards("fuel")
                .map { (_, event) -> event.streamId }
                .findFirstAndClose()!!

        assertThat(response.getFirstHeader("Location")?.value, equalTo(server.uri("/_api/fuel/${streamId.id}").toString()))
    }

    @Test fun post_new_fuel_purchase_inaccessible_without_login() {
        val newFuelPurchase = FuelResources.NewFuelPurchase(
                fuelVolume = 45.67,
                cost = MonetaryAmount("GBP", 99.87),
                odometer = 111234.0,
                fullFill = true,
                location = "Somewhere",
                geoLocation = null
        )

        val response = execute(HttpPost(server.uri("/_api/fuel")).apply {
            entity = JacksonEntity(newFuelPurchase)
        })
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_FORBIDDEN))
    }

    @Test fun get_fuel_purchase_inaccessible_without_login() {
        val purchaseId = UUID.randomUUID()

        server.instance<EventStreamWriter>().write(StreamId("fuel", purchaseId.toString()), listOf(
                EventCodecs.encode(FuelPurchased(
                        timestamp = Instant.parse("2017-04-01T20:46:00Z"),
                        userId = UUID.randomUUID(),
                        fuelVolume = 45.67,
                        cost = MonetaryAmount("GBP", 99.87),
                        odometer = 111234.0,
                        fullFill = true,
                        location = "Somewhere",
                        geoLocation = null)
                        , emptyMetadata)
        ))

        val response = execute(HttpGet("/_api/fuel/$purchaseId"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_FORBIDDEN))
    }

    @Test fun get_fuel_purchases_for_current_user() {
        val purchaseId = UUID.randomUUID()
        loginAsNewUser()

        server.instance<EventStreamWriter>().write(StreamId("fuel", purchaseId.toString()), listOf(
                EventCodecs.encode(FuelPurchased(
                        Instant.now(server.clock),
                        currentUser.userId,
                        50.0,
                        MonetaryAmount("GBP", 100.0),
                        110000.0,
                        true,
                        "Some place",
                        geoLocation = null
                ), emptyMetadata)
        ))

        val response = execute(HttpGet("/_api/fuel"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        val fuelRecords = response.readJson<Collection<FuelRecord>>().toList()
        assertThat(fuelRecords[0].userId, equalTo(currentUser.userId))
        assertThat(fuelRecords[0].fuelPurchaseId, equalTo(purchaseId))
        assertThat(fuelRecords[0].fuelVolume, equalTo(50.0))
    }

    private fun serverEvents(): List<DecodedEvent> = server.instance<EventCategoryReader>().readCategoryForwards("fuel")
            .map { (_, event) -> DecodedEvent(event.streamId.category, EventCodecs.decode(event), defaultObjectMapper.readTree(event.metadata.openStream())) }
            .toListAndClose()

    data class DecodedEvent(val category: String, val data: Event, val metadata: JsonNode)
}
