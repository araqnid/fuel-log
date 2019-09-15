package org.araqnid.fuellog

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.or
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.araqnid.eventstore.Blob
import org.araqnid.hamkrest.json.bytesEquivalentTo
import org.araqnid.hamkrest.json.equivalentTo
import org.araqnid.hamkrest.json.jsonBytes
import org.araqnid.hamkrest.json.jsonNull
import org.araqnid.hamkrest.json.jsonNumber
import org.araqnid.hamkrest.json.jsonString

fun hasMimeType(mimeType: String): Matcher<HttpEntity> {
    return has("content-type", { entity -> ContentType.getOrDefault(entity).mimeType },
            Matcher(String::equalsIgnoreCase, mimeType))
}

fun jsonScalar() = jsonString(anything) or jsonNumber(anything) or jsonNull()

fun jsonBlobEquivalentTo(referenceJson: String): Matcher<Blob> =
        equivalentTo(referenceJson) { it.openStream().use { stream -> ObjectMapper().readTree(stream) } }

private fun String.equalsIgnoreCase(str: String) = equals(str, ignoreCase = true)

fun hasJson(referenceJson: String): Matcher<HttpEntity> =
        hasMimeType("application/json") and has(HttpEntity::bytes, bytesEquivalentTo(referenceJson))

fun hasJson(jsonMatcher: Matcher<JsonNode>): Matcher<HttpEntity> =
        hasMimeType("application/json") and has(HttpEntity::bytes, jsonBytes(jsonMatcher))
