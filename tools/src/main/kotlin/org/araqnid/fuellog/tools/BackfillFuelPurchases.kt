package org.araqnid.fuellog.tools

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.common.base.Splitter
import com.google.common.io.Resources
import org.araqnid.eventstore.StreamId
import org.araqnid.eventstore.filesystem.FilesystemEventSource
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.EventMetadata
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import org.araqnid.fuellog.events.UserExternalIdAssigned
import org.araqnid.fuellog.filterNotNull
import org.araqnid.fuellog.toListAndClose
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

object BackfillFuelPurchases {
    @JvmStatic fun main(args: Array<String>) {
        val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/google/112460655559871226975")
        val eventsDirectory = Paths.get(if (args.isNotEmpty()) args[0] else "events")
        with(eventsDirectory.resolve("fuel")) { if (Files.exists(this)) deleteRecursively(this) }
        val userId = FilesystemEventSource(eventsDirectory, Clock.systemDefaultZone())
                .categoryReader.readCategoryForwards("user")
                .map { it.event }
                .map{
                    val event = EventCodecs.decode(it)
                    if (event is UserExternalIdAssigned && event.externalId == externalId) {
                        UUID.fromString(it.streamId.id)
                    }
                    else {
                        null
                    }
                }
                .filterNotNull()
                .findFirst()
                .orElseThrow { IllegalStateException("user with external ID $externalId not found") }

        val metadata = object : EventMetadata {
        }

        Resources.asCharSource(Resources.getResource("purchases.txt"), UTF_8).readLines().forEach { line ->
            val parts = Splitter.onPattern("[\t ]+").limit(6).splitToList(line)
            val timestamp = LocalDate.parse(parts[0]).atStartOfDay(ZoneId.of("Europe/London")).toInstant()
            val purchaseEvent = FuelPurchased(timestamp, userId, parts[2].toDouble(), MonetaryAmount("GBP", parts[4].toDouble()),
                    parts[1].toDouble() * KM_PER_MILE, parts[3].isYes(), parts[5])
            val purchaseId = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)
                    .generate("https://fuel.araqnid.org/fuel/?user_id=$userId&timestamp=${purchaseEvent.timestamp}&odometer=${purchaseEvent.odometer}")
            FilesystemEventSource(eventsDirectory, Clock.fixed(timestamp, ZoneId.of("Europe/London")))
                    .streamWriter
                    .write(StreamId("fuel", purchaseId.toString()),
                            listOf(EventCodecs.encode(purchaseEvent, metadata)))
        }
    }

    const val KM_PER_MILE: Double = 1.60934

    fun deleteRecursively(parent: Path) {
        Files.list(parent).toListAndClose().forEach { member ->
            if (Files.isDirectory(member))
                deleteRecursively(member)
            else
                Files.delete(member)
        }
        Files.delete(parent)
    }

    fun String.isYes(): Boolean = this.equals("yes", ignoreCase = true)
}
