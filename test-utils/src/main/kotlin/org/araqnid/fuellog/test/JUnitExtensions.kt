package org.araqnid.fuellog.test

import org.junit.Assert

inline fun <reified T : Throwable> assertThrows(message: String = "", crossinline block: () -> Unit) {
    Assert.assertThrows(message, T::class.java) { block() }
}
