package org.araqnid.fuellog

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.has
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.entity.ContentType
import org.araqnid.hamkrest.json.bytesEquivalentTo
import org.araqnid.hamkrest.json.jsonBytes

fun hasMimeType(mimeType: String): Matcher<HttpEntity> =
        has("content-type", { entity -> ContentType.getOrDefault(entity).mimeType },
                Matcher(String::equalsIgnoreCase, mimeType))


fun hasJson(referenceJson: String): Matcher<HttpEntity> =
        hasMimeType("application/json") and has(HttpEntity::bytes, bytesEquivalentTo(referenceJson))

fun hasJson(jsonMatcher: Matcher<JsonNode>): Matcher<HttpEntity> =
        hasMimeType("application/json") and has(HttpEntity::bytes, jsonBytes(jsonMatcher))


val HttpResponse.isSuccess
    get() = statusLine.statusCode in 200..299

val HttpResponse.isOK
    get() = statusLine.statusCode == 200

fun isJsonOk(referenceJson: String): Matcher<HttpResponse> =
        Matcher(HttpResponse::isOK) and has(HttpResponse::getEntity, hasJson(referenceJson))

fun isJsonOk(jsonMatcher: Matcher<JsonNode>): Matcher<HttpResponse> =
        Matcher(HttpResponse::isOK) and has(HttpResponse::getEntity, hasJson(jsonMatcher))

private fun String.equalsIgnoreCase(str: String) = equals(str, ignoreCase = true)
