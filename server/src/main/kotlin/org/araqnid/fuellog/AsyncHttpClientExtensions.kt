package org.araqnid.fuellog

import com.fasterxml.jackson.databind.ObjectReader
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.entity.ContentType
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.util.EntityUtils
import java.net.URI
import javax.ws.rs.BadRequestException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

private val permittedJsonMimeTypes = setOf("text/javascript", "application/json")

fun <T : Any> parseJsonResponse(uri: URI,
                                response: HttpResponse,
                                objectReader: ObjectReader
): T {
    if (response.statusLine.statusCode != HttpStatus.SC_OK)
        throw BadRequestException("$uri: ${response.statusLine}")

    val contentType = ContentType.get(response.entity) ?: throw BadRequestException("$uri: no content-type")
    if (contentType.mimeType.toLowerCase() !in permittedJsonMimeTypes)
        throw BadRequestException("$uri: unhandled content-type: $contentType")

    return objectReader.readValue(response.entity.content)
}

fun <T : Any> parseJsonResponse(
    uri: URI,
    response: HttpResponse,
    deserializer: DeserializationStrategy<T>
): T {
    if (response.statusLine.statusCode != HttpStatus.SC_OK)
        throw BadRequestException("$uri: ${response.statusLine}")

    val contentType = ContentType.get(response.entity) ?: throw BadRequestException("$uri: no content-type")
    if (contentType.mimeType.toLowerCase() !in permittedJsonMimeTypes)
        throw BadRequestException("$uri: unhandled content-type: $contentType")

    return Json { ignoreUnknownKeys = true }.decodeFromString(deserializer, EntityUtils.toString(response.entity))
}
