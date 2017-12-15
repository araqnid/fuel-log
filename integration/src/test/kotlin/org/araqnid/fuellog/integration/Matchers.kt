package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.has
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.rules.ExpectedException

fun hasMimeType(mimeType: String): Matcher<HttpEntity> {
    return has("content-type", { entity -> ContentType.getOrDefault(entity).mimeType },
            Matcher(String::equalsIgnoreCase, mimeType))
}

private fun String.equalsIgnoreCase(str: String) = equals(str, ignoreCase = true)
