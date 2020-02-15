package org.araqnid.fuellog

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.HttpEntity
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

fun <T : HttpRequest> T.accepting(mimeType: String): T = apply {
    addHeader("Accept", mimeType)
}

val HttpEntity.bytes: ByteArray
    get() = EntityUtils.toByteArray(this)

val HttpEntity.text: String
    get() = EntityUtils.toString(this)

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(Jdk8Module())
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

inline fun <reified T : Any> HttpResponse.readJson(): T = defaultObjectMapper.readValue(entity.content)

fun formEntity(params: Map<String, String>) = UrlEncodedFormEntity(params.entries.map { (k, v) -> BasicNameValuePair(k, v) })
