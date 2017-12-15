package org.araqnid.fuellog

import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.jboss.resteasy.spi.ResteasyProviderFactory
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import javax.ws.rs.container.AsyncResponse
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

class ResteasyAsync<in T>(
        private val asyncResponse: AsyncResponse,
        private val data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        private val executor: Executor = ForkJoinPool.commonPool()
) : CoroutineDispatcher(), Continuation<T> {
    override val context: CoroutineContext = this@ResteasyAsync

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.execute {
            ResteasyProviderFactory.pushContextDataMap(data)
            try {
                block.run()
            } finally {
                ResteasyProviderFactory.removeContextDataLevel()
            }
        }
    }

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
}

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, executor: Executor, block: suspend () -> T) {
    block.startCoroutine(ResteasyAsync(asyncResponse, executor = executor))
}
