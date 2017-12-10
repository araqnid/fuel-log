package org.araqnid.fuellog

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.DefaultDispatcher
import org.jboss.resteasy.spi.ResteasyProviderFactory
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor

internal inline fun <T> withThreadContextData(data: Map<Class<*>, Any>, block: () -> T): T {
    ResteasyProviderFactory.pushContextDataMap(data)
    try {
        return block()
    } finally {
        ResteasyProviderFactory.removeContextDataLevel()
    }
}

private class ResteasyAsyncContinuation<T>(private val data: Map<Class<*>, Any>, val cont: Continuation<T>) : Continuation<T> by cont {
    override fun resume(value: T) {
        withThreadContextData(data) {
            cont.resume(value)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        withThreadContextData(data) {
            cont.resumeWithException(exception)
        }
    }
}

class ResteasyAsync(
        private val data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        private val next: CoroutineDispatcher = DefaultDispatcher
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return next.interceptContinuation(ResteasyAsyncContinuation(data, continuation))
    }
}
