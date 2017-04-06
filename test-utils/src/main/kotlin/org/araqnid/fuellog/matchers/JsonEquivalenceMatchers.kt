package org.araqnid.fuellog.matchers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import java.io.IOException

fun jsonBytesEquivalentTo(referenceJson: String): Matcher<ByteArray> = jsonRepresentationEquivalentTo(referenceJson, { ObjectMapper().readTree(it) })

fun jsonTextEquivalentTo(referenceJson: String): Matcher<String> = jsonRepresentationEquivalentTo(referenceJson, { ObjectMapper().readTree(it) })

fun jsonNodeEquivalentTo(referenceJson: String): Matcher<JsonNode> = jsonRepresentationEquivalentTo(referenceJson, { it })

fun <T> jsonRepresentationEquivalentTo(referenceJson: String, parser: (T) -> JsonNode): Matcher<T> {
    val referenceJsonNode = try {
        ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .readTree(referenceJson)
    } catch (e: IOException) {
        throw IllegalArgumentException("Reference JSON is invalid", e)
    }
    return object : TypeSafeDiagnosingMatcher<T>() {
        override fun matchesSafely(input: T, mismatchDescription: Description): Boolean {
            val actualJsonNode = try {
                parser(input)
            } catch (e: IOException) {
                mismatchDescription.appendText("JSON was invalid: $e");
                return false
            }
            mismatchDescription.appendText("JSON was ").appendValue(actualJsonNode)
            return actualJsonNode == referenceJsonNode
        }

        override fun describeTo(description: Description) {
            description.appendText("is JSON ").appendValue(referenceJson)
        }
    }
}
