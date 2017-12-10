package org.araqnid.fuellog

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.DefaultDispatcher
import org.jboss.resteasy.spi.ResteasyProviderFactory
import java.util.concurrent.Executor
import javax.ws.rs.container.AsyncResponse
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

internal inline fun <T> withThreadContextData(data: Map<Class<*>, Any>, block: () -> T): T {
    ResteasyProviderFactory.pushContextDataMap(data)
    try {
        return block()
    } finally {
        ResteasyProviderFactory.removeContextDataLevel()
    }
}

class ResteasyAsync(
        private val data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        private val next: CoroutineDispatcher = DefaultDispatcher
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        next.dispatch(context, Runnable {
            withThreadContextData(data) {
                block.run()
            }
        })
    }
}

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, executor: Executor, block: suspend () -> T) {
    block.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = ResteasyAsync()

        override fun resume(value: T) {
            executor.execute {
                asyncResponse.resume(value)
            }
        }

        override fun resumeWithException(exception: Throwable) {
            executor.execute {
                asyncResponse.resume(exception)
            }
        }
    })
}
