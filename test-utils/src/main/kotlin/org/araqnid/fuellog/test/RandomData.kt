package org.araqnid.fuellog.test

import java.util.*

private val tlds = listOf("com", "net", "org", "co.uk", "org.uk")

private val RANDOM = Random()

private val alphabet = "abcdefghijklmnopqrstuvwxyz"

fun randomInteger() = RANDOM.nextInt()

fun randomInteger(max: Int) = RANDOM.nextInt(max)

@JvmOverloads
fun randomString(prefix: String = "", len: Int = 10): String {
    val builder = if (prefix.isEmpty())
        StringBuilder(len)
    else
        StringBuilder(len + prefix.length + 1).apply {
            append(prefix)
            append('-')
        }

    for (i in 1..len) {
        builder.append(alphabet[RANDOM.nextInt(alphabet.length)])
    }
    return builder.toString()
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
