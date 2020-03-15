package org.araqnid.fuellog

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.timgroup.clocks.testing.ManualClock
import org.araqnid.eventstore.InMemoryEventSource
import org.araqnid.eventstore.NoSuchStreamException
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.Coordinates
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.EventMetadata
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import org.araqnid.fuellog.hamkrest.containsInAnyOrder
import org.araqnid.fuellog.test.assertThrows
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.util.UUID

class FuelRepositoryTest {
    val clock = ManualClock.initiallyAt(Clock.systemDefaultZone())
    val eventSource = InMemoryEventSource(clock)
    val repo = FuelRepository(eventSource.streamReader, eventSource.categoryReader)

    val emptyMetadata = object : EventMetadata { }

    @Test fun gets_purchase_by_id() {
        val purchaseId = randomUUID()

        val timestamp = Instant.parse("2017-06-08T16:54:00Z")
        val userId = randomUUID()
        val fuelVolume = 45.67
        val currency = "GBP"
        val amount = 62.13
        val odometer = 111023.0
        val location = "Someplace"
        val coords = Coordinates(51.4, 0.02)

        submit(StreamId("fuel", purchaseId.toString()),
                FuelPurchased(timestamp, userId, fuelVolume, MonetaryAmount(currency, amount), odometer, true, location, coords))

        val purchase = repo[purchaseId]
        assertThat(purchase.fuelPurchaseId, equalTo(purchaseId))
        assertThat(purchase.cost.currency, equalTo(currency))
        assertThat(purchase.cost.amount, equalTo(amount))
        assertThat(purchase.fuelVolume, equalTo(fuelVolume))
        assertThat(purchase.odometer, equalTo(odometer))
        assertThat(purchase.locationString, equalTo(location))
        assertThat(purchase.geoLocation, equalTo(coords))
    }

    @Test fun unknown_purchase_throws_error() {
        assertThrows<NoSuchStreamException> {
            repo[randomUUID()]
        }
    }

    @Test fun gets_purchases_by_user_id() {
        val userId1 = randomUUID()
        val userId2 = randomUUID()
        val purchaseId1 = randomUUID()
        val purchaseId2 = randomUUID()
        val purchaseId3 = randomUUID()

        val fuelVolume = 45.67
        val currency = "GBP"
        val amount = 62.13
        val odometer = 111023.0
        val location = "Someplace"

        submit(StreamId("fuel", purchaseId1.toString()),
                FuelPurchased(Instant.parse("2017-06-08T16:54:00Z"), userId1, fuelVolume, MonetaryAmount(currency, amount), odometer, true, location, null))
        submit(StreamId("fuel", purchaseId2.toString()),
                FuelPurchased(Instant.parse("2017-06-09T16:54:00Z"), userId1, fuelVolume, MonetaryAmount(currency, amount), odometer, true, location, null))
        submit(StreamId("fuel", purchaseId3.toString()),
                FuelPurchased(Instant.parse("2017-06-10T16:54:00Z"), userId2, fuelVolume, MonetaryAmount(currency, amount), odometer, true, location, null))

        assertThat(repo.byUserId(userId1).map { it.fuelPurchaseId }, containsInAnyOrder(equalTo(purchaseId1), equalTo(purchaseId2)))
        assertThat(repo.byUserId(userId2).map { it.fuelPurchaseId }, containsInAnyOrder(equalTo(purchaseId3)))
    }

    fun submit(streamId: StreamId, event: Event) {
        eventSource.streamWriter.write(streamId, listOf(EventCodecs.encode(event, emptyMetadata)))
    }

    fun randomUUID(): UUID = UUID.randomUUID()
}
