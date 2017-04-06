package org.araqnid.fuellog

import java.util.Objects
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream

inline fun <T : AutoCloseable, R> T.useResource(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {
            e.addSuppressed(closeException)
        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}

fun <T, R> Stream<T>.collectAndClose(collector: Collector<in T, *, out R>): R = useResource { it.collect(collector) }
fun <T> Stream<T>.toListAndClose(): List<T> = collectAndClose(Collectors.toList())
fun <T> Stream<T>.forEachOrderedAndClose(action: (T) -> Unit) = useResource { it.forEachOrdered(action) }
fun <T> Stream<T>.forEachAndClose(action: (T) -> Unit) = useResource { it.forEach(action) }
fun <T> Stream<T>.findFirstAndClose(): T? = useResource { it.findFirst().orElse(null) }
fun <T> Stream<T?>.filterNotNull(): Stream<T> {
    @Suppress("UNCHECKED_CAST")
    return this.filter(Objects::nonNull) as Stream<T>
}
fun <T> Stream<T>.onlyElement(): T? = with(toListAndClose()) {
    if (size != 1) throw IllegalArgumentException("stream produced more than one element: $this")
    get(0)
}
