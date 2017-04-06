package org.araqnid.fuellog.integration

import org.apache.http.HttpStatus
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.cookie.Cookie
import org.apache.http.message.BasicNameValuePair
import org.araqnid.eventstore.EventReader
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.UserExternalIdAssigned
import org.araqnid.fuellog.matchers.jsonTextEquivalentTo
import org.araqnid.fuellog.toListAndClose
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Test
import java.net.URI

class IdentityResourcesIntegrationTest : IntegrationTest() {
    @Test fun identity_resource_returns_nothing_for_unauthenticated_user() {
        execute(HttpGet("/_api/user/identity"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType("application/json"))
        assertThat(response.entity.text, jsonTextEquivalentTo("{ user_info: null }"))
        assertThat(httpContext.cookieStore.cookies, not(contains(cookieNamed("JSESSIONID"))))
    }

    @Test fun associating_user_creates_registration_event() {
        execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to "test0", "name" to "Test User"))
        })
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))

        val userEvents = fetchUserEvents()
        assertThat(userEvents.map { it.event }, hasItem(UserExternalIdAssigned(URI.create("https://fuel.araqnid.org/_api/user/identity/test/test0")) as Event))
        val userId = userEvents[0].streamId.id

        assertThat(response.entity, hasMimeType("application/json"))
        assertThat(response.entity.text, jsonTextEquivalentTo("{ user_id: '$userId', name: 'Test User', realm: 'TEST', picture: null }"))
    }

    @Test fun associated_user_returned_from_identity_resource() {
        execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to "test0", "name" to "Test User"))
        })
        execute(HttpGet("/_api/user/identity"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))

        val userEvents = fetchUserEvents()
        val userId = userEvents[0].streamId.id

        assertThat(response.entity, hasMimeType("application/json"))
        assertThat(response.entity.text, jsonTextEquivalentTo("{ user_info: { user_id: '$userId', name: 'Test User', realm: 'TEST', picture: null } }"))
        assertThat(httpContext.cookieStore.cookies, contains(cookieNamed("JSESSIONID")))
    }

    @Test fun after_deleting_identity_identity_resource_returns_empty_again() {
        execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to "test0", "name" to "Test User"))
        })
        execute(HttpGet("/_api/user/identity"))
        execute(HttpDelete("/_api/user/identity"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_NO_CONTENT))
        execute(HttpGet("/_api/user/identity"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_OK))
        assertThat(response.entity, hasMimeType("application/json"))
        assertThat(response.entity.text, jsonTextEquivalentTo("{ user_info: null }"))
    }

    @Test fun deleting_identity_does_not_crash_if_no_user_set() {
        execute(HttpDelete("/_api/user/identity"))
        assertThat(response.statusLine.statusCode, equalTo(HttpStatus.SC_NO_CONTENT))
    }

    data class DecodedEvent(val streamId: StreamId, val event: Event)

    private fun fetchUserEvents(): List<DecodedEvent> {
        return server.instance<EventReader>()
                .readAllForwards()
                .map { it.event }
                .filter { it.streamId.category == "user" }
                .map { DecodedEvent(it.streamId, EventCodecs.decode(it)) }
                .toListAndClose()
    }

    private fun cookieNamed(name: String): Matcher<Cookie> = object : TypeSafeDiagnosingMatcher<Cookie>() {
        override fun matchesSafely(item: Cookie, mismatchDescription: Description): Boolean {
            mismatchDescription.appendText("cookie was ").appendValue(item)
            return item.name == name
        }

        override fun describeTo(description: Description) {
            description.appendText("cookie named ").appendValue(name)
        }
    }
}