package org.araqnid.fuellog.events

import java.time.Instant
import java.util.UUID

data class FuelPurchased(
        val timestamp: Instant,
        val userId: UUID,
        val fuelVolume: Double /* litres */,
        val cost: MonetaryAmount,
        val odometer: Double /* km */,
        val fullFill: Boolean,
        val location: String) : Event

data class MonetaryAmount(val currency: String, val amount: Double)
