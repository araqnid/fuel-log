package org.araqnid.fuellog

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.DefaultDispatcher
import org.jboss.resteasy.spi.ResteasyProviderFactory
import kotlin.coroutines.experimental.CoroutineContext

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
