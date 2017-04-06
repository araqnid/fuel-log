package org.araqnid.fuellog

import org.araqnid.eventstore.EventStreamReader
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class FuelRepository @Inject constructor(val streamReader: EventStreamReader) {
    operator fun get(fuelPurchaseId: UUID): FuelRecord {
        var record: FuelRecord? = null
        val streamId = StreamId("fuel", fuelPurchaseId.toString())
        streamReader.readStreamForwards(streamId)
                .map { EventCodecs.decode(it.event) }
                .forEachOrderedAndClose { event ->
                    if (record == null) {
                        when (event) {
                            is FuelPurchased -> record = FuelRecord.from(fuelPurchaseId, event)
                            else -> throw RuntimeException("unhandled initial event in $streamId")
                        }
                    }
                    else {
                        record = record!!.accept(event)
                    }
                }
        return record!!
    }
}

data class FuelRecord(val fuelPurchaseId: UUID,
                      val purchasedAt: Instant,
                      val fuelVolume: Double,
                      val cost: MonetaryAmount,
                      val odometer: Double /* km */,
                      val fullFill: Boolean,
                      val locationString: String) {
    fun accept(event: Event): FuelRecord {
        when (event) {
            is FuelPurchased -> throw IllegalArgumentException("Multiple FuelPurchased events")
            else -> throw IllegalArgumentException("Unhandled event: $event")
        }
    }

    companion object {
        fun from(id: UUID, purchaseEvent: FuelPurchased): FuelRecord {
            return FuelRecord(fuelPurchaseId = id, purchasedAt = purchaseEvent.timestamp,
                    fuelVolume = purchaseEvent.fuelVolume, cost = purchaseEvent.cost,
                    odometer = purchaseEvent.odometer, fullFill = purchaseEvent.fullFill,
                    locationString = purchaseEvent.location)
        }
    }
}