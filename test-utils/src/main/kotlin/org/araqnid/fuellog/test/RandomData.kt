package org.araqnid.fuellog.test

import java.util.*

private val tlds = listOf("com", "net", "org", "co.uk", "org.uk")

private val RANDOM = Random()

private val alphabet = "abcdefghijklmnopqrstuvwxyz"

fun randomInteger() = RANDOM.nextInt()

fun randomInteger(max: Int) = RANDOM.nextInt(max)

@JvmOverloads
fun randomString(prefix: String = "", len: Int = 10): String {
    return buildString(len + prefix.length + 1) {
        if (prefix.isNotEmpty()) {
            append(prefix)
            append('-')
        }
        for (i in 1..len) {
            append(alphabet[RANDOM.nextInt(alphabet.length)])
        }
    }
}

inline fun <reified T : Enum<T>> randomEnumInstance(): T = EnumSet.allOf(T::class.java).pickOne()

fun <T : Enum<T>> randomOtherInstanceOfEnum(excluded: T): T = EnumSet.complementOf(EnumSet.of(excluded)).pickOne()

fun randomDomain() = "${randomString()}.example.${tlds.pickOne()}"

fun randomEmailAddress() = "${randomString()}@${randomDomain()}"

fun randomUriString() = "http://www.${randomDomain()}/${randomString()}"

fun <T> Collection<T>.pickOne(): T {
    if (this is List<T>) {
        return get(RANDOM.nextInt(size))
    }

    var index = RANDOM.nextInt(size)
    val iterator = iterator()
    while (index-- > 0) {
        iterator.next()
    }
    return iterator.next()
}
