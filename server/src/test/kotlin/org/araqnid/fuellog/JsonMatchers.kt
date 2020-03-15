package org.araqnid.fuellog

import com.fasterxml.jackson.databind.ObjectMapper
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.or
import org.araqnid.eventstore.Blob
import org.araqnid.hamkrest.json.equivalentTo
import org.araqnid.hamkrest.json.jsonNull
import org.araqnid.hamkrest.json.jsonNumber
import org.araqnid.hamkrest.json.jsonString

fun jsonScalar() = jsonString(anything) or jsonNumber(anything) or jsonNull()

fun jsonBlobEquivalentTo(referenceJson: String): Matcher<Blob> =
        equivalentTo(referenceJson) { it.openStream().use { stream -> ObjectMapper().readTree(stream) } }
