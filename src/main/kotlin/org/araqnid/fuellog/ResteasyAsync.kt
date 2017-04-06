package org.araqnid.fuellog

import org.jboss.resteasy.spi.ResteasyProviderFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

internal inline fun <T> withThreadContextData(data: Map<Class<*>, Any>, block: () -> T): T {
    ResteasyProviderFactory.pushContextDataMap(data)
    try {
        return block()
    } finally {
        ResteasyProviderFactory.removeContextDataLevel()
    }
}

internal fun <T> supplyAsyncWithContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(), supplier: Supplier<T>): CompletionStage<T> {
    return CompletableFuture.supplyAsync(supplier.withContextData(data)).withContextData(data)
}

internal fun <T> supplyAsyncWithContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(), supplier: Supplier<T>, executor: Executor): CompletionStage<T> {
    return CompletableFuture.supplyAsync(supplier.withContextData(data), executor).withContextData(data)
}

internal fun runAsyncWithContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(), runnable: Runnable): CompletionStage<Void> {
    return CompletableFuture.runAsync(runnable.withContextData(data)).withContextData(data)
}

internal fun runAsyncWithContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(), runnable: Runnable, executor: Executor): CompletionStage<Void> {
    return CompletableFuture.runAsync(runnable.withContextData(data), executor).withContextData(data)
}

internal fun Executor.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = Executor { command ->
    withThreadContextData(data) {
        command.run()
    }
}

internal fun Runnable.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = Runnable {
    withThreadContextData(data) {
        this@withContextData.run()
    }
}

internal fun <T> Supplier<T>.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = Supplier<T> {
    withThreadContextData(data) {
        this@withContextData.get()
    }
}

internal fun <T> Consumer<T>.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = Consumer<T> { t ->
    withThreadContextData(data) {
        this@withContextData.accept(t)
    }
}

internal fun <T, U> BiConsumer<T, U>.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = BiConsumer<T, U> { t, u ->
    withThreadContextData(data) {
        this@withContextData.accept(t, u)
    }
}

internal fun <T, U> Function<T, U>.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = Function<T, U> { t ->
    withThreadContextData(data) {
        this@withContextData.apply(t)
    }
}

internal fun <T, U, V> BiFunction<T, U, V>.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = BiFunction<T, U, V> { t, u ->
    withThreadContextData(data) {
        this@withContextData.apply(t, u)
    }
}

internal fun <T> CompletionStage<T>.withContextData(data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap()) = wrapWithContextData(data, this)

internal fun <T> wrapWithContextData(data: Map<Class<*>, Any>, stage: CompletionStage<T>): CompletionStage<T> {
    return object : CompletionStage<T> {
        override fun thenRun(action: Runnable): CompletionStage<Void> = stage.thenRun(action.withContextData(data)).withContextData(data)
        override fun thenRunAsync(action: Runnable): CompletionStage<Void> = stage.thenRun(action.withContextData(data)).withContextData(data)
        override fun thenRunAsync(action: Runnable, executor: Executor): CompletionStage<Void> = stage.thenRunAsync(action.withContextData(data), executor).withContextData(data)

        override fun thenAccept(action: Consumer<in T>): CompletionStage<Void> = stage.thenAccept(action.withContextData(data)).withContextData(data)
        override fun thenAcceptAsync(action: Consumer<in T>): CompletionStage<Void> = stage.thenAcceptAsync(action.withContextData(data)).withContextData(data)
        override fun thenAcceptAsync(action: Consumer<in T>, executor: Executor): CompletionStage<Void> = stage.thenAcceptAsync(action.withContextData(data), executor).withContextData(data)

        override fun <U> thenApply(fn: Function<in T, out U>): CompletionStage<U> = stage.thenApply(fn.withContextData(data)).withContextData(data)
        override fun <U> thenApplyAsync(fn: Function<in T, out U>): CompletionStage<U> = stage.thenApplyAsync(fn.withContextData(data)).withContextData(data)
        override fun <U> thenApplyAsync(fn: Function<in T, out U>, executor: Executor): CompletionStage<U> = stage.thenApplyAsync(fn.withContextData(data), executor).withContextData(data)

        override fun <U> thenCompose(fn: Function<in T, out CompletionStage<U>>): CompletionStage<U> = stage.thenCompose(fn.withContextData(data)).withContextData(data)
        override fun <U> thenComposeAsync(fn: Function<in T, out CompletionStage<U>>): CompletionStage<U> = stage.thenComposeAsync(fn.withContextData(data)).withContextData(data)
        override fun <U> thenComposeAsync(fn: Function<in T, out CompletionStage<U>>, executor: Executor): CompletionStage<U> = stage.thenComposeAsync(fn.withContextData(data), executor).withContextData(data)

        override fun whenComplete(action: BiConsumer<in T, in Throwable?>): CompletionStage<T> = stage.whenComplete(action.withContextData(data)).withContextData(data)
        override fun whenCompleteAsync(action: BiConsumer<in T, in Throwable?>): CompletionStage<T> = stage.whenCompleteAsync(action.withContextData(data)).withContextData(data)
        override fun whenCompleteAsync(action: BiConsumer<in T, in Throwable?>, executor: Executor): CompletionStage<T> = stage.whenCompleteAsync(action.withContextData(data), executor).withContextData(data)

        override fun <U> handle(fn: BiFunction<in T, Throwable?, out U>): CompletionStage<U> = stage.handle(fn.withContextData(data)).withContextData(data)
        override fun <U> handleAsync(fn: BiFunction<in T, Throwable?, out U>): CompletionStage<U> = stage.handleAsync(fn.withContextData(data)).withContextData(data)
        override fun <U> handleAsync(fn: BiFunction<in T, Throwable?, out U>, executor: Executor): CompletionStage<U> = stage.handleAsync(fn.withContextData(data), executor).withContextData(data)

        override fun exceptionally(fn: Function<Throwable, out T>): CompletionStage<T> = stage.exceptionally(fn.withContextData(data)).withContextData(data)


        override fun runAfterBoth(other: CompletionStage<*>, action: Runnable): CompletionStage<Void> = stage.runAfterBoth(other, action.withContextData(data)).withContextData(data)
        override fun runAfterBothAsync(other: CompletionStage<*>, action: Runnable): CompletionStage<Void> = stage.runAfterBothAsync(other, action.withContextData(data)).withContextData(data)
        override fun runAfterBothAsync(other: CompletionStage<*>, action: Runnable, executor: Executor): CompletionStage<Void> = stage.runAfterBothAsync(other, action.withContextData(data), executor).withContextData(data)

        override fun <U> thenAcceptBoth(other: CompletionStage<out U>, action: BiConsumer<in T, in U>): CompletionStage<Void> = stage.thenAcceptBoth(other, action.withContextData(data)).withContextData(data)
        override fun <U> thenAcceptBothAsync(other: CompletionStage<out U>, action: BiConsumer<in T, in U>): CompletionStage<Void> = stage.thenAcceptBothAsync(other, action.withContextData(data)).withContextData(data)
        override fun <U> thenAcceptBothAsync(other: CompletionStage<out U>, action: BiConsumer<in T, in U>, executor: Executor): CompletionStage<Void> = stage.thenAcceptBothAsync(other, action.withContextData(data), executor).withContextData(data)

        override fun <U, V> thenCombine(other: CompletionStage<out U>, fn: BiFunction<in T, in U, out V>): CompletionStage<V> = stage.thenCombine(other, fn.withContextData(data)).withContextData(data)
        override fun <U, V> thenCombineAsync(other: CompletionStage<out U>, fn: BiFunction<in T, in U, out V>): CompletionStage<V> = stage.thenCombineAsync(other, fn.withContextData(data)).withContextData(data)
        override fun <U, V> thenCombineAsync(other: CompletionStage<out U>, fn: BiFunction<in T, in U, out V>, executor: Executor): CompletionStage<V> = stage.thenCombineAsync(other, fn.withContextData(data), executor).withContextData(data)


        override fun runAfterEither(other: CompletionStage<*>, action: Runnable): CompletionStage<Void> = stage.runAfterEither(other, action.withContextData(data)).withContextData(data)
        override fun runAfterEitherAsync(other: CompletionStage<*>, action: Runnable): CompletionStage<Void> = stage.runAfterEitherAsync(other, action.withContextData(data)).withContextData(data)
        override fun runAfterEitherAsync(other: CompletionStage<*>, action: Runnable, executor: Executor): CompletionStage<Void> = stage.runAfterEitherAsync(other, action.withContextData(data), executor).withContextData(data)

        override fun acceptEither(other: CompletionStage<out T>, action: Consumer<in T>): CompletionStage<Void> = stage.acceptEither(other, action.withContextData(data)).withContextData(data)
        override fun acceptEitherAsync(other: CompletionStage<out T>, action: Consumer<in T>): CompletionStage<Void> = stage.acceptEitherAsync(other, action.withContextData(data)).withContextData(data)
        override fun acceptEitherAsync(other: CompletionStage<out T>, action: Consumer<in T>, executor: Executor): CompletionStage<Void> = stage.acceptEitherAsync(other, action.withContextData(data), executor).withContextData(data)

        override fun <U> applyToEither(other: CompletionStage<out T>, fn: Function<in T, U>): CompletionStage<U> = stage.applyToEither(other, fn.withContextData(data)).withContextData(data)
        override fun <U> applyToEitherAsync(other: CompletionStage<out T>, fn: Function<in T, U>): CompletionStage<U> = stage.applyToEitherAsync(other, fn.withContextData(data)).withContextData(data)
        override fun <U> applyToEitherAsync(other: CompletionStage<out T>, fn: Function<in T, U>, executor: Executor): CompletionStage<U> = stage.applyToEitherAsync(other, fn.withContextData(data), executor).withContextData(data)

        override fun toCompletableFuture(): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            stage.whenComplete { result, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                }
                else {
                    future.complete(result)
                }
            }
            return future
        }
    }
}
