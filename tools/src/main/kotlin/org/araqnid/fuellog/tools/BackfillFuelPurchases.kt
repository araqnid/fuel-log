package org.araqnid.fuellog.tools

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.common.base.Splitter
import com.google.common.io.MoreFiles
import com.google.common.io.Resources
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.stream.consumeAsFlow
import org.araqnid.eventstore.StreamId
import org.araqnid.eventstore.filesystem.TieredFilesystemEventSource
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.EventMetadata
import org.araqnid.fuellog.events.FuelPurchased
import org.araqnid.fuellog.events.MonetaryAmount
import org.araqnid.fuellog.events.UserExternalIdAssigned
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

object BackfillFuelPurchases {
    @JvmStatic fun main(args: Array<String>) {
        val externalId = URI.create("https://fuel.araqnid.org/_api/user/identity/google/112460655559871226975")
        val eventsDirectory = Paths.get(if (args.isNotEmpty()) args[0] else "events")
        eventsDirectory.resolve("fuel").takeIf { Files.exists(it) }?.let { MoreFiles.deleteRecursively(it) }
        val userId = runBlocking {
            TieredFilesystemEventSource(eventsDirectory, Clock.systemDefaultZone())
                .categoryReader.readCategoryForwards("user")
                .map { (_, eventRecord) ->
                    val event = EventCodecs.decode(eventRecord)
                    if (event is UserExternalIdAssigned && event.externalId == externalId) {
                        UUID.fromString(eventRecord.streamId.id)
                    } else {
                        null
                    }
                }
                .consumeAsFlow()
                .filterNotNull()
                .first()
        }

        val metadata = object : EventMetadata {
        }

        Resources.asCharSource(Resources.getResource("purchases.txt"), UTF_8).readLines().forEach { line ->
            val parts = Splitter.onPattern("[\t ]+").limit(6).splitToList(line)
            val timestamp = LocalDate.parse(parts[0]).atStartOfDay(ZoneId.of("Europe/London")).toInstant()
            val purchaseEvent = FuelPurchased(timestamp, userId, parts[2].toDouble(), MonetaryAmount("GBP", parts[4].toDouble()),
                    parts[1].toDouble() * KM_PER_MILE, parts[3].isYes(), parts[5], null)
            val purchaseId = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)
                    .generate("https://fuel.araqnid.org/fuel/?user_id=$userId&timestamp=${purchaseEvent.timestamp}&odometer=${purchaseEvent.odometer}")
            TieredFilesystemEventSource(eventsDirectory, Clock.fixed(timestamp, ZoneId.of("Europe/London")))
                    .streamWriter
                    .write(StreamId("fuel", purchaseId.toString()),
                            listOf(EventCodecs.encode(purchaseEvent, metadata)))
        }
    }

    const val KM_PER_MILE: Double = 1.60934

    fun String.isYes(): Boolean = this.equals("yes", ignoreCase = true)
}
