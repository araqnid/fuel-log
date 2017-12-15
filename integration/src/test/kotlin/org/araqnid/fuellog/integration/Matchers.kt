package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.has
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType

fun hasMimeType(mimeType: String): Matcher<HttpEntity> {
    return has("content-type", { entity -> ContentType.getOrDefault(entity).mimeType },
            Matcher(String::equalsIgnoreCase, mimeType))
}

private fun String.equalsIgnoreCase(str: String) = equals(str, ignoreCase = true)
