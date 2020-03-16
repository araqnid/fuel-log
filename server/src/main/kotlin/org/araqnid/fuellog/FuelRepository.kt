package org.araqnid.fuellog

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.araqnid.eventstore.EventCategoryReader
import org.araqnid.eventstore.EventStreamReader
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.Coordinates
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class FuelRepository @Inject constructor(val streamReader: EventStreamReader, val categoryReader: EventCategoryReader) {
    operator fun get(fuelPurchaseId: UUID): FuelRecord {
        var record: FuelRecord? = null
        val streamId = StreamId("fuel", fuelPurchaseId.toString())
        runBlocking {
            streamReader.readStreamForwards(streamId)
                .map { EventCodecs.decode(it.event) }
                .collect { event ->
                    record = if (record == null) {
                        when (event) {
                            is FuelPurchased -> FuelRecord.from(fuelPurchaseId, event)
                            else -> throw RuntimeException("unhandled initial event in $streamId")
                        }
                    } else {
                        record!!.accept(event)
                    }
                }
        }
        return record!!
    }

    fun byUserId(userId: UUID): Collection<FuelRecord> {
        val records = mutableMapOf<UUID, FuelRecord>()
        runBlocking {
            categoryReader.readCategoryForwards("fuel")
                .collect { (_, eventRecord) ->
                    val streamId = eventRecord.streamId
                    val event = EventCodecs.decode(eventRecord)
                    val purchaseId = UUID.fromString(streamId.id)
                    val existingRecord = records[purchaseId]
                    when {
                        existingRecord != null -> existingRecord.accept(event)
                        event is FuelPurchased && event.userId == userId ->
                            records[purchaseId] = FuelRecord.from(purchaseId, event)
                        // else ignore
                    }
                }
        }
        return records.values
    }
}

data class FuelRecord(val fuelPurchaseId: UUID,
                      val userId: UUID,
                      val purchasedAt: Instant,
                      val fuelVolume: Double,
                      val cost: MonetaryAmount,
                      val odometer: Double /* km */,
                      val fullFill: Boolean,
                      val locationString: String,
                      val geoLocation: Coordinates?) {
    fun accept(event: Event): FuelRecord {
        when (event) {
            is FuelPurchased -> throw IllegalArgumentException("Multiple FuelPurchased events")
            else -> throw IllegalArgumentException("Unhandled event: $event")
        }
    }

    companion object {
        fun from(id: UUID, purchaseEvent: FuelPurchased): FuelRecord {
            return FuelRecord(fuelPurchaseId = id, purchasedAt = purchaseEvent.timestamp,
                    userId = purchaseEvent.userId,
                    fuelVolume = purchaseEvent.fuelVolume, cost = purchaseEvent.cost,
                    odometer = purchaseEvent.odometer, fullFill = purchaseEvent.fullFill,
                    locationString = purchaseEvent.location, geoLocation = purchaseEvent.geoLocation)
        }
    }
}
