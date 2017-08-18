package org.araqnid.fuellog

import java.util.Objects
import java.util.stream.Stream
import kotlin.streams.toList

fun <T> Stream<T>.toListAndClose(): List<T> = use { it.toList() }
fun <T> Stream<T>.forEachOrderedAndClose(action: (T) -> Unit) = use { it.forEachOrdered(action) }
fun <T> Stream<T>.findFirstAndClose(): T? = use { it.findFirst().orElse(null) }
fun <T> Stream<T?>.filterNotNull(): Stream<T> {
    @Suppress("UNCHECKED_CAST")
    return this.filter(Objects::nonNull) as Stream<T>
}
