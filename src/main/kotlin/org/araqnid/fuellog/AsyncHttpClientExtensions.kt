package org.araqnid.fuellog

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.nio.client.HttpAsyncClient
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

fun HttpAsyncClient.executeAsyncHttpRequest(request: HttpUriRequest): CompletionStage<HttpResponse> {
    return CompletableFuture<HttpResponse>().apply {
        val apacheCallback = object : FutureCallback<HttpResponse> {
            override fun completed(result: HttpResponse) {
                this@apply.complete(result)
            }

            override fun failed(ex: Exception) {
                this@apply.completeExceptionally(ex)
            }

            override fun cancelled() {
                this@apply.cancel(false)
            }
        }
        this@executeAsyncHttpRequest.execute(request, apacheCallback)
    }.withContextData()
}
