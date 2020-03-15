package org.araqnid.fuellog

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.stream.consumeAsFlow
import org.araqnid.eventstore.EventCategoryReader
import org.araqnid.eventstore.EventReader
import org.araqnid.eventstore.EventRecord
import org.araqnid.eventstore.EventStreamReader
import org.araqnid.eventstore.StreamId

fun EventReader.events(): List<EventRecord> {
    return runBlocking {
        readAllForwards().map { it.event }.consumeAsFlow().toList()
    }
}

fun EventCategoryReader.events(category: String): List<EventRecord> {
    return runBlocking {
        readCategoryForwards(category).map { it.event }.consumeAsFlow().toList()
    }
}

fun EventStreamReader.events(streamId: StreamId): List<EventRecord> {
    return runBlocking {
        readStreamForwards(streamId).map { it.event }.consumeAsFlow().toList()
    }
}

fun EventStreamReader.events(streamCategory: String, streamId: String): List<EventRecord> {
    return runBlocking {
        readStreamForwards(StreamId(streamCategory, streamId)).map { it.event }.consumeAsFlow().toList()
    }
}
