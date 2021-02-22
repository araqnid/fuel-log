@file:OptIn(ExperimentalSerializationApi::class)

package org.araqnid.fuellog

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.time.Instant

@Serializer(forClass = Instant::class)
object EpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor =
        PrimitiveSerialDescriptor("org.araqnid.fuellog.EpochSecondsSerializer", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochSecond)
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.ofEpochSecond(decoder.decodeLong())
    }
}

@Serializer(forClass = URI::class)
object URISerializer : KSerializer<URI> {
    override val descriptor = PrimitiveSerialDescriptor("org.araqnid.fuellog.URISerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URI {
        return URI(decoder.decodeString())
    }
}
