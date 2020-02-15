package org.araqnid.fuellog

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasElement
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.cookie.Cookie
import org.araqnid.eventstore.EventReader
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.UserExternalIdAssigned
import org.junit.Rule
import org.junit.Test
import java.net.URI

class IdentityResourcesTest {
    @get:Rule
    val server = ServerRunner()

    @Test
    fun identity_resource_returns_nothing_for_unauthenticated_user() {
        val response = server.execute(HttpGet("/_api/user/identity"))
        assertThat(response, isJsonOk("{ user_info: null }"))
        assertThat(server.httpContext.cookieStore.cookies, !anyElement(has(Cookie::getName, equalTo("JSESSIONID"))))
    }

    @Test
    fun associating_user_creates_registration_event() {
        val response = server.execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to "test0", "name" to "Test User"))
        })
        assertThat(response, Matcher(HttpResponse::isSuccess))

        val userEvents = fetchUserEvents()
        assertThat(userEvents.map { it.event }, hasElement(UserExternalIdAssigned(URI.create("https://fuel.araqnid.org/_api/user/identity/test/test0")) as Event))
        val userId = userEvents[0].streamId.id

        assertThat(response, isJsonOk("{ user_id: '$userId', name: 'Test User', realm: 'TEST', picture: null }"))
    }

    @Test fun associated_user_returned_from_identity_resource() {
        server.execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to "test0", "name" to "Test User"))
        })
        val response = server.execute(HttpGet("/_api/user/identity"))
        assertThat(response, Matcher(HttpResponse::isSuccess))

        val userEvents = fetchUserEvents()
        val userId = userEvents[0].streamId.id

        assertThat(response,
                isJsonOk("{ user_info: { user_id: '$userId', name: 'Test User', realm: 'TEST', picture: null } }"))
        assertThat(server.httpContext.cookieStore.cookies, anyElement(has(Cookie::getName, equalTo("JSESSIONID"))))
    }

    @Test fun after_deleting_identity_identity_resource_returns_empty_again() {
        server.execute(HttpPost("/_api/user/identity/test").apply {
            entity = formEntity(mapOf("identifier" to "test0", "name" to "Test User"))
        })
        server.execute(HttpGet("/_api/user/identity"))
        val deleteResponse = server.execute(HttpDelete("/_api/user/identity"))
        assertThat(deleteResponse, Matcher(HttpResponse::isSuccess))
        val getResponse = server.execute(HttpGet("/_api/user/identity"))
        assertThat(getResponse, isJsonOk("{ user_info: null }"))
    }

    @Test fun deleting_identity_does_not_crash_if_no_user_set() {
        val response = server.execute(HttpDelete("/_api/user/identity"))
        assertThat(response, Matcher(HttpResponse::isSuccess))
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

}
