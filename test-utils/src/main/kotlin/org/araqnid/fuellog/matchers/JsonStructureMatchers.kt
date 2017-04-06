package org.araqnid.fuellog.matchers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Sets
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.hamcrest.TypeSafeDiagnosingMatcher

fun <T : JsonNode> jsonBytesStructuredAs(nodeMatcher: Matcher<T>) = object : TypeSafeDiagnosingMatcher<ByteArray>() {
    override fun matchesSafely(item: ByteArray, mismatchDescription: Description): Boolean {
        val node: JsonNode = objectMapper.readTree(item)
        nodeMatcher.describeMismatch(node, mismatchDescription)
        return nodeMatcher.matches(node)
    }

    override fun describeTo(description: Description) {
        description.appendText("JSON bytes ").appendDescriptionOf(nodeMatcher)
    }
}

fun <T : JsonNode> jsonTextStructuredAs(nodeMatcher: Matcher<T>): Matcher<String> = object : TypeSafeDiagnosingMatcher<String>() {
    override fun matchesSafely(item: String, mismatchDescription: Description): Boolean {
        val node: JsonNode = objectMapper.readTree(item)
        nodeMatcher.describeMismatch(node, mismatchDescription)
        return nodeMatcher.matches(node)
    }

    override fun describeTo(description: Description) {
        description.appendText("JSON text ").appendDescriptionOf(nodeMatcher)
    }
}

fun jsonObject() = ObjectNodeMatcher()

class ObjectNodeMatcher : TypeSafeDiagnosingMatcher<JsonNode>() {
    private val propertyMatchers = LinkedHashMap<String, Matcher<out JsonNode>>()
    private var failOnUnexpectedProperties = true

    override fun matchesSafely(item: JsonNode, mismatchDescription: Description): Boolean {
        val remainingFieldNames = Sets.newHashSet(item.fieldNames())
        propertyMatchers.forEach { (fieldName, valueMatcher) ->
            if (!item.has(fieldName)) {
                mismatchDescription.appendText(fieldName).appendText(" was not present")
                return false
            }
            val valueNode: JsonNode = item.get(fieldName)
            if (!valueMatcher.matches(valueNode)) {
                mismatchDescription.appendText(fieldName).appendText(": ")
                valueMatcher.describeMismatch(valueNode, mismatchDescription)
                return false
            }
            remainingFieldNames.remove(fieldName)
        }
        if (failOnUnexpectedProperties && !remainingFieldNames.isEmpty()) {
            mismatchDescription.appendText("unexpected properties: ").appendValue(remainingFieldNames)
            return false
        }
        return true
    }

    override fun describeTo(description: Description) {
        description.appendText("{ ")
        var first = true
        for ((fieldName, valueMatcher) in propertyMatchers) {
            if (first)
                first = false
            else
                description.appendText(", ")
            description.appendValue(fieldName).appendText(": ").appendDescriptionOf(valueMatcher)
        }
        description.appendText(" }")
    }

    fun withProperty(key: String, value: Matcher<out JsonNode>) = apply {
        if (propertyMatchers.containsKey(key)) throw IllegalStateException("Matcher for property '$key' already specified")
        propertyMatchers[key] = value
    }

    fun withProperty(key: String, value: String) = withProperty(key, jsonString(value))

    fun withPropertyJSON(key: String, json: String) = withProperty(key, jsonNodeEquivalentTo(json))

    fun withAnyOtherProperties() = apply {
        failOnUnexpectedProperties = false
    }
}

fun jsonString(matcher: Matcher<String>): Matcher<JsonNode> = object : TypeSafeDiagnosingMatcher<JsonNode>() {
    override fun matchesSafely(item: JsonNode, mismatchDescription: Description): Boolean {
        mismatchDescription.appendText("string was: ").appendValue(item.asText())
        return matcher.matches(item.asText())
    }

    override fun describeTo(description: Description) {
        description.appendText("string ").appendDescriptionOf(matcher)
    }
}
fun jsonString(value: String) = jsonString(equalTo(value))
fun jsonAnyString() = jsonString(Matchers.any(String::class.java))

fun jsonNull(): Matcher<JsonNode> = object : TypeSafeDiagnosingMatcher<JsonNode>() {
    override fun matchesSafely(item: JsonNode, mismatchDescription: Description): Boolean {
        mismatchDescription.appendText("was ").appendValue(item)
        return item.isNull
    }

    override fun describeTo(description: Description) {
        description.appendText("null")
    }
}

private val objectMapper = ObjectMapper()
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)