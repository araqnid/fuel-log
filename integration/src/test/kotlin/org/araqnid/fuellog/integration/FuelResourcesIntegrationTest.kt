package org.araqnid.fuellog.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.araqnid.eventstore.Blob
import org.araqnid.eventstore.EventCategoryReader
import org.araqnid.eventstore.EventStreamWriter
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.FuelRecord
import org.araqnid.fuellog.FuelResources
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.EventMetadata
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import org.araqnid.fuellog.findFirstAndClose
import org.araqnid.fuellog.toListAndClose
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Test
import java.io.InputStream
import java.time.Instant
import java.util.UUID

class FuelResourcesIntegrationTest : IntegrationTest() {
    val objectMapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    val emptyMetadata = object : EventMetadata {}

    @Test fun get_fuel_purchase() {
        val purchaseId = UUID.randomUUID()
        val user = loginAsNewUser()

        server.instance<EventStreamWriter>().write(StreamId("fuel", purchaseId.toString()), listOf(
                EventCodecs.encode(FuelPurchased(
                        Instant.now(server.clock),
                        user.userId,
                        50.0,
                        MonetaryAmount("GBP", 100.0),
                        110000.0,
                        true,
                        "Some place"
                ), emptyMetadata)
        ))

        execute(HttpGet("/_api/fuel/$purchaseId"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        val fuelRecord = readJsonResponse<FuelRecord>()
        assertThat(fuelRecord.userId, equalTo(user.userId))
        assertThat(fuelRecord.fuelPurchaseId, equalTo(purchaseId))
        assertThat(fuelRecord.fuelVolume, equalTo(50.0))
    }

    @Test fun post_new_fuel_purchase() {
        val user = loginAsNewUser()

        val newFuelPurchase = FuelResources.NewFuelPurchase(
                fuelVolume = 45.67,
                cost = MonetaryAmount("GBP", 99.87),
                odometer = 111234.0,
                fullFill = true,
                location = "Somewhere"
        )

        execute(HttpPost("/_api/fuel").apply { entity = JacksonEntity(newFuelPurchase, objectMapper) })
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_CREATED))

        val fuelPurchasedEvent = FuelPurchased(
                timestamp = server.clock.instant(),
                userId = user.userId,
                fuelVolume = 45.67,
                cost = MonetaryAmount("GBP", 99.87),
                odometer = 111234.0,
                fullFill = true,
                location = "Somewhere")

        assertThat(serverEvents(), contains(
                DecodedEvent.matcher(equalTo("fuel"), equalTo(fuelPurchasedEvent), hasEntry("client_ip", "127.0.0.1"))
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
                location = "Somewhere"
        )

        execute(HttpPost(server.uri("/_api/fuel")).apply {
            entity = JacksonEntity(newFuelPurchase, objectMapper)
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
                        location = "Somewhere")
                        , emptyMetadata)
        ))

        execute(HttpGet("/_api/fuel/$purchaseId"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_FORBIDDEN))
    }

    @Test fun get_fuel_purchases_for_current_user() {
        val purchaseId = UUID.randomUUID()
        val user = loginAsNewUser()

        server.instance<EventStreamWriter>().write(StreamId("fuel", purchaseId.toString()), listOf(
                EventCodecs.encode(FuelPurchased(
                        Instant.now(server.clock),
                        user.userId,
                        50.0,
                        MonetaryAmount("GBP", 100.0),
                        110000.0,
                        true,
                        "Some place"
                ), emptyMetadata)
        ))

        execute(HttpGet("/_api/fuel"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        val fuelRecords = readJsonResponse<Collection<FuelRecord>>().toList()
        assertThat(fuelRecords[0].userId, equalTo(user.userId))
        assertThat(fuelRecords[0].fuelPurchaseId, equalTo(purchaseId))
        assertThat(fuelRecords[0].fuelVolume, equalTo(50.0))
    }

    fun serverEvents(): List<DecodedEvent> = server.instance<EventCategoryReader>().readCategoryForwards("fuel")
            .map { (_, event) -> DecodedEvent(event.streamId.category, EventCodecs.decode(event), event.metadata.parseJsonAsGeneric()) }
            .toListAndClose()

    data class DecodedEvent(val category: String, val data: Event, val metadata: Map<String, Any>) {
        companion object {
            fun matcher(category: Matcher<String>, data: Matcher<Event>, metadata: Matcher<Map<out String, Any>>): Matcher<DecodedEvent> {
                return object : TypeSafeDiagnosingMatcher<DecodedEvent>() {
                    override fun matchesSafely(item: DecodedEvent, mismatchDescription: Description): Boolean {
                        if (!category.matches(item.category)) {
                            mismatchDescription.appendText("category ")
                            category.describeMismatch(item.category, mismatchDescription)
                            return false
                        }
                        if (!data.matches(item.data)) {
                            mismatchDescription.appendText("data ")
                            category.describeMismatch(item.data, mismatchDescription)
                            return false
                        }
                        if (!metadata.matches(item.metadata)) {
                            mismatchDescription.appendText("metadata ")
                            category.describeMismatch(item.metadata, mismatchDescription)
                            return false
                        }
                        return true
                    }

                    override fun describeTo(description: Description) {
                        description.appendText("data with category ").appendDescriptionOf(category)
                                .appendText(", data ").appendDescriptionOf(data)
                                .appendText(", metadata ").appendDescriptionOf(metadata)
                    }
                }
            }
        }
    }

    private inline fun <reified T> typeReference() : TypeReference<T> = object : TypeReference<T>() {}

    private fun Blob.parseJsonAsGeneric(): Map<String, Any> = objectMapper.readerFor(typeReference<Map<String, Any>>()).readValue(this.openStream())

    private inline fun <reified T> readJsonResponse(): T = readJson(response.entity.content)

    private inline fun <reified T> readJson(input: InputStream): T = objectMapper.readerFor(typeReference<T>()).readValue(input)
}
