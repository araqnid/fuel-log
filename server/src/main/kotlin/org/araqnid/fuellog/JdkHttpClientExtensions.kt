package org.araqnid.fuellog

import kotlinx.coroutines.future.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
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
    deserializer: DeserializationStrategy<T>,
    requestConfig: (HttpRequest.Builder).() -> Unit
): T = getJson(deserializer, httpRequest(requestConfig))

suspend fun <T : Any> HttpClient.getJson(
    deserializer: DeserializationStrategy<T>,
    request: HttpRequest
): T = sendAsync(request, JsonBodyHandler(request.uri(), deserializer)).await().body()

private class JsonBodyHandler<T>(
    private val uri: URI,
    private val deserializer: DeserializationStrategy<T>
) : HttpResponse.BodyHandler<T> {
    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<T> {
        if (responseInfo.statusCode() != 200)
            throw BadRequestException("$uri: (${responseInfo.statusCode()}")

        responseInfo.headers().firstValue("content-type")
            .orElseThrow { BadRequestException("$uri: no content-type") }
            .let {
                val contentType = ContentType.parse(it)
                if (contentType.mimeType.toLowerCase() !in permittedJsonMimeTypes)
                    throw BadRequestException("$uri: unhandled content-type: $contentType")
            }

        return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofString(UTF_8)) { string ->
            format.decodeFromString(deserializer, string)
        }
    }

    companion object {
        private val format = Json { ignoreUnknownKeys = true }
    }
}
