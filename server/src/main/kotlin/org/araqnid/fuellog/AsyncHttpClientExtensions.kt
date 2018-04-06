package org.araqnid.fuellog

import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.nio.client.HttpAsyncClient
import java.lang.Exception

suspend fun HttpAsyncClient.executeAsyncHttpRequest(request: HttpUriRequest): HttpResponse {
    return suspendCancellableCoroutine { cont ->
        val apacheCallback = object : FutureCallback<HttpResponse> {
            override fun completed(result: HttpResponse) {
                cont.resume(result)
            }

            override fun failed(ex: Exception) {
                cont.resumeWithException(ex)
            }

            override fun cancelled() {
                cont.cancel()
            }
        }
        this@executeAsyncHttpRequest.execute(request, apacheCallback)
    }
}