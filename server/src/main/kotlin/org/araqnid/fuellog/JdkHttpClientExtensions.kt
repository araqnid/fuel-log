package org.araqnid.fuellog

import com.fasterxml.jackson.databind.ObjectReader
import kotlinx.coroutines.future.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.ws.rs.BadRequestException
import kotlin.text.Charsets.UTF_8

inline fun httpRequest(block: (HttpRequest.Builder).() -> Unit): HttpRequest {
    val builder = HttpRequest.newBuilder()
    builder.block()
    return builder.build()
}

fun HttpRequest.Builder.POSTFormData(vararg formParameters: Pair<String, String>) =
    POSTFormData(formParameters.toList())

fun HttpRequest.Builder.POSTFormData(formParameters: Collection<Pair<String, String>>) {
    POST(
        HttpRequest.BodyPublishers.ofString(
            URLEncodedUtils.format(
                formParameters.map { (name, value) -> BasicNameValuePair(name, value) },
                HTTP.DEF_CONTENT_CHARSET
            )
        )
    )
    header(
        "Content-Type", ContentType.create(
            URLEncodedUtils.CONTENT_TYPE,
            HTTP.DEF_CONTENT_CHARSET
        ).toString()
    )
}

inline fun httpClient(block: (HttpClient.Builder).() -> Unit): HttpClient {
    val builder = HttpClient.newBuilder()
    builder.block()
    return builder.build()
}

private val permittedJsonMimeTypes = setOf("text/javascript", "application/json")

suspend inline fun <T : Any> HttpClient.getJson(
    objectReader: ObjectReader,
    requestConfig: (HttpRequest.Builder).() -> Unit
): T {
    val request = httpRequest(requestConfig)
    return parseJsonResponse(
        request.uri(),
        sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).await(),
        objectReader
    )
}

suspend inline fun <T : Any> HttpClient.getJson(
    deserializer: DeserializationStrategy<T>,
    requestConfig: (HttpRequest.Builder).() -> Unit
): T {
    val request = httpRequest(requestConfig)
    return parseJsonResponse(
        request.uri(),
        sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).await(),
        deserializer
    )
}

fun <T : Any> parseJsonResponse(
    uri: URI,
    response: HttpResponse<ByteArray>,
    objectReader: ObjectReader
): T {
    if (response.statusCode() != HttpStatus.SC_OK)
        throw BadRequestException("$uri: ${response.statusCode()}")

    response.headers().firstValue("content-type")
        .orElseThrow { BadRequestException("$uri: no content-type") }
        .let {
            val contentType = ContentType.parse(it)
            if (contentType.mimeType.toLowerCase() !in permittedJsonMimeTypes)
                throw BadRequestException("$uri: unhandled content-type: $contentType")
        }

    return objectReader.readValue(response.body())
}

fun <T : Any> parseJsonResponse(
    uri: URI,
    response: HttpResponse<ByteArray>,
    deserializer: DeserializationStrategy<T>
): T {
    if (response.statusCode() != HttpStatus.SC_OK)
        throw BadRequestException("$uri: ${response.statusCode()}")

    response.headers().firstValue("content-type")
        .orElseThrow { BadRequestException("$uri: no content-type") }
        .let {
            val contentType = ContentType.parse(it)
            if (contentType.mimeType.toLowerCase() !in permittedJsonMimeTypes)
                throw BadRequestException("$uri: unhandled content-type: $contentType")
        }

    return Json { ignoreUnknownKeys = true }.decodeFromString(deserializer, response.body().toString(UTF_8))
}
