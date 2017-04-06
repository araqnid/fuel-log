package org.araqnid.fuellog.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.araqnid.eventstore.Blob
import org.araqnid.eventstore.EventRecord
import org.araqnid.eventstore.NewEvent
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object EventCodecs {
    val objectMapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .registerModule(GuavaModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    val objectReaders = ConcurrentHashMap<String, ObjectReader>()

    fun decode(input: EventRecord): Event {
        val reader = objectReaders.computeIfAbsent(input.type, this::createObjectReader)
        try {
            return reader.readValue<Event>(input.data.openStream())
        } catch (e: Exception) {
            throw RuntimeException("Failed to decode event $input", e)
        }
    }

    fun encode(data: Event, metadata: EventMetadata) = NewEvent(data.javaClass.simpleName, encode(data), encode(metadata))

    fun encode(input: Event): Blob {
        try {
            return Blob(objectMapper.writeValueAsBytes(input))
        } catch (e: IOException) {
            throw RuntimeException("Failed to encode event $input", e)
        }
    }

    fun encode(input: EventMetadata): Blob {
        try {
            return Blob(objectMapper.writeValueAsBytes(input))
        } catch (e: IOException) {
            throw RuntimeException("Failed to encode metadata $input", e)
        }
    }

    private fun createObjectReader(eventType: String): ObjectReader {
        try {
            val clazzName = javaClass.`package`.name + "." + eventType
            val clazz = Class.forName(clazzName)
            if (!Event::class.java.isAssignableFrom(clazz))
                throw IllegalArgumentException("Unhandled event type: $eventType ($clazzName exists, but it doesn't implement Event)")
            return objectMapper.readerFor(clazz)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Unhandled event type: $eventType")
        } catch (e: Exception) {
            throw RuntimeException("Unable to initialise JSON decoder for $eventType", e)
        }
    }
}