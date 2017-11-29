package org.araqnid.fuellog

import com.fasterxml.jackson.databind.ObjectMapper
import org.araqnid.eventstore.Blob
import org.araqnid.eventstore.EventRecord
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.FacebookProfileChanged
import org.araqnid.fuellog.events.FacebookProfileData
import org.araqnid.fuellog.events.GoogleProfileChanged
import org.araqnid.fuellog.events.GoogleProfileData
import org.araqnid.fuellog.events.UserExternalIdAssigned
import org.araqnid.fuellog.events.UserNameChanged
import org.araqnid.fuellog.matchers.jsonRepresentationEquivalentTo
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Instant
import kotlin.test.assertEquals

class EventCodecsTest {
    @Test
    fun `decodes UserExternalIdAssigned event`() {
        val eventRecord = EventRecord(StreamId("user", "218c61de-fca8-573d-b15c-b5976296fc82"), 0L, Instant.EPOCH,
                "UserExternalIdAssigned",
                Blob.fromString("""{"external_id":"https://fuel.araqnid.org/_api/user/identity/google/112460655559871226975"}"""),
                Blob.fromString("""{"client_ip":"127.0.0.1","user_agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36"}"""))
        assertEquals(UserExternalIdAssigned(URI.create("https://fuel.araqnid.org/_api/user/identity/google/112460655559871226975")),
                EventCodecs.decode(eventRecord))
    }

    @Test
    fun `encodes UserExternalIdAssigned event`() {
        assertThat(EventCodecs.encode(UserExternalIdAssigned(URI.create("https://fuel.araqnid.org/_api/user/identity/google/112460655559871226975"))),
                jsonBlobEquivalentTo("""{"external_id":"https://fuel.araqnid.org/_api/user/identity/google/112460655559871226975"}"""))
    }

    @Test
    fun `encodes metadata`() {
        assertThat(EventCodecs.encode(RequestMetadata(clientIp = "127.0.0.1", userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")),
                jsonBlobEquivalentTo("""{"client_ip":"127.0.0.1","user_agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36"}"""))
    }

    @Test
    fun `decodes UserNameChanged event`() {
        val eventRecord = EventRecord(StreamId("user", "218c61de-fca8-573d-b15c-b5976296fc82"), 0L, Instant.EPOCH,
                "UserNameChanged",
                Blob.fromString("""{"new_value":"Steve Haslam"}"""),
                metadata = Blob.empty)
        assertEquals(UserNameChanged("Steve Haslam"),
                EventCodecs.decode(eventRecord))
    }

    @Test
    fun `encodes UserNameChanged event`() {
        assertThat(EventCodecs.encode(UserNameChanged("Steve Haslam")),
                jsonBlobEquivalentTo("""{"new_value":"Steve Haslam"}"""))
    }

    @Test
    fun `decodes GoogleProfileChanged event`() {
        val eventRecord = EventRecord(StreamId("user", "218c61de-fca8-573d-b15c-b5976296fc82"), 0L, Instant.EPOCH,
                "GoogleProfileChanged",
                Blob.fromString("""{"new_value":{"given_name":"Steve","family_name":"Haslam","picture":"https://lh6.googleusercontent.com/-533s8Xng1pI/AAAAAAAAAAI/AAAAAAAAMKg/khgLL7xBRk4/s96-c/photo.jpg"}}"""),
                metadata = Blob.empty)
        assertEquals(GoogleProfileChanged(GoogleProfileData("Steve", "Haslam", URI.create("https://lh6.googleusercontent.com/-533s8Xng1pI/AAAAAAAAAAI/AAAAAAAAMKg/khgLL7xBRk4/s96-c/photo.jpg"))),
                EventCodecs.decode(eventRecord))
    }

    @Test
    fun `encodes GoogleProfileChanged event`() {
        assertThat(EventCodecs.encode(GoogleProfileChanged(GoogleProfileData("Steve", "Haslam", URI.create("https://lh6.googleusercontent.com/-533s8Xng1pI/AAAAAAAAAAI/AAAAAAAAMKg/khgLL7xBRk4/s96-c/photo.jpg")))),
                jsonBlobEquivalentTo("""{"new_value":{"given_name":"Steve","family_name":"Haslam","picture":"https://lh6.googleusercontent.com/-533s8Xng1pI/AAAAAAAAAAI/AAAAAAAAMKg/khgLL7xBRk4/s96-c/photo.jpg"}}"""))
    }

    @Test
    fun `decodes FacebookProfileChanged event`() {
        val eventRecord = EventRecord(StreamId("user", "34db44f9-b9ec-5c1d-98d7-0a1c02913fb6"), 0L, Instant.EPOCH,
                "FacebookProfileChanged",
                Blob.fromString("""{"new_value":{"picture":"https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/22365519_10155849960609669_4611817305999182053_n.jpg?oh=dbbd4324f5adf21776510ed0cd5dd3b9&oe=5A9F07D2"}}"""),
                metadata = Blob.empty)
        assertEquals(FacebookProfileChanged(FacebookProfileData(URI.create("https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/22365519_10155849960609669_4611817305999182053_n.jpg?oh=dbbd4324f5adf21776510ed0cd5dd3b9&oe=5A9F07D2"))),
                EventCodecs.decode(eventRecord))
    }

    @Test
    fun `encodes FacebookProfileChanged event`() {
        assertThat(EventCodecs.encode(FacebookProfileChanged(FacebookProfileData(URI.create("https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/22365519_10155849960609669_4611817305999182053_n.jpg?oh=dbbd4324f5adf21776510ed0cd5dd3b9&oe=5A9F07D2")))),
                jsonBlobEquivalentTo("""{"new_value":{"picture":"https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/22365519_10155849960609669_4611817305999182053_n.jpg?oh=dbbd4324f5adf21776510ed0cd5dd3b9&oe=5A9F07D2"}}"""))
    }
}

fun jsonBlobEquivalentTo(referenceJson: String): Matcher<Blob> = jsonRepresentationEquivalentTo(referenceJson, { it.openStream().use { stream -> ObjectMapper().readTree(stream) } })
