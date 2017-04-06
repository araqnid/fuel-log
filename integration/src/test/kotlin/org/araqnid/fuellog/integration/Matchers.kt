package org.araqnid.fuellog.integration

import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher

fun hasMimeType(mimeType: String): Matcher<HttpEntity> {
    return object : TypeSafeDiagnosingMatcher<HttpEntity>() {
        override fun matchesSafely(item: HttpEntity, mismatchDescription: Description): Boolean {
            val contentType = ContentType.getOrDefault(item)
            mismatchDescription.appendText("content type was ").appendValue(contentType)
            return contentType.mimeType.equals(mimeType, ignoreCase = true)
        }

        override fun describeTo(description: Description) {
            description.appendText("entity with MIME type ").appendValue(mimeType)
        }
    }
}
