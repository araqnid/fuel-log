package org.araqnid.fuellog

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.araqnid.eventstore.EventReader
import org.araqnid.eventstore.EventRecord
import org.araqnid.eventstore.subscription.SnapshotEventProcessor
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.FuelPurchased
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.Clock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

//private val snapshotCompatibilityVersion = 2017033001L
private val snapshotCompatibilityVersion = System.currentTimeMillis()

@Singleton
class EventProcessorImpl @Inject constructor(@Named("SNAPSHOT_SPOOL") baseDirectory: String,
                                             storeReader: EventReader,
                                             clock: Clock)
    : SnapshotEventProcessor(Paths.get(baseDirectory), objectMapper, storeReader.positionCodec, storeReader.emptyStorePosition, snapshotCompatibilityVersion, clock) {
    private val logger = LoggerFactory.getLogger(EventProcessorImpl::class.java)

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .registerModule(GuavaModule())
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    }

    override fun process(eventRecord: EventRecord) {
        when (val event = EventCodecs.decode(eventRecord)) {
            is FuelPurchased -> logger.info("Ignoring $event")
            else -> logger.warn("Unhandled $event")
        }
    }

    override fun loadSnapshotJson(jsonParser: JsonParser) {
        logger.info("TODO: load snapshot json")
    }

    override fun saveSnapshotJson(jsonGenerator: JsonGenerator) {
        logger.info("TODO: save snapshot json")
    }
}
