package org.araqnid.fuellog

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import org.araqnid.eventstore.Blob
import org.araqnid.eventstore.EventRecord
import org.araqnid.eventstore.InMemoryEventSource
import org.araqnid.eventstore.NewEvent
import org.araqnid.eventstore.StreamId
import org.araqnid.fuellog.matchers.jsonBytesEquivalentTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Clock
import java.util.UUID

private val emptyMetadata = RequestMetadata("10.0.0.0", null)

class UserRepositoryTest {
    val eventSource = InMemoryEventSource(Clock.systemDefaultZone())
    val repo = UserRepository(eventSource.categoryReader, eventSource.streamReader, eventSource.streamWriter, eventSource.clock)

    @Test fun empty_repository_returns_nothing_for_external_id_lookup() {
        assertNull(repo.findUserByExternalId(URI.create("http://www.example.com")))
    }

    @Test fun create_user_with_external_id() {
        val externalId = URI.create("http://www.example.com")
        repo.findOrCreateUserByExternalId(externalId, emptyMetadata)
        assertThat(eventSource.storeReader.readAllForwards().map { it.event }.toListAndClose(),
                contains(eventRecord(StreamId("user", externalId.toUUID().toString()), 0, "UserExternalIdAssigned",
                                        jsonBytesEquivalentTo("{ external_id: '$externalId' }"))))
    }

    @Test fun find_user_by_id() {
        val externalId = URI.create("http://www.example.com")
        val userId = UUID.randomUUID()
        eventSource.streamWriter.write(StreamId("user", userId.toString()), listOf(
                NewEvent("UserExternalIdAssigned", jsonBlob("{ external_id: '$externalId' }"), Blob.empty)
        ))

        val user = repo.findUser(userId).assertNotNull()
        assertThat(user.userId, equalTo(userId))
        assertThat(user.externalId, equalTo(externalId))
    }

    @Test fun find_user_by_external_id() {
        val externalId = URI.create("http://www.example.com")
        val userId = UUID.randomUUID()
        eventSource.streamWriter.write(StreamId("user", userId.toString()), listOf(
                NewEvent("UserExternalIdAssigned", jsonBlob("{ external_id: '$externalId' }"), Blob.empty)
        ))

        val user = repo.findUserByExternalId(externalId).assertNotNull()
        assertThat(user.userId, equalTo(userId))
        assertThat(user.externalId, equalTo(externalId))
    }

    @Test fun read_change_to_user_name() {
        val externalId = URI.create("http://www.example.com")
        val userId = UUID.randomUUID()
        eventSource.streamWriter.write(StreamId("user", userId.toString()), listOf(
                NewEvent("UserExternalIdAssigned", jsonBlob("{ external_id: '$externalId' }"), Blob.empty),
                NewEvent("UserNameChanged", jsonBlob("{ new_value: 'A User' }"), Blob.empty)
        ))

        assertThat(repo.findUser(userId)!!.name, equalTo("A User"))
    }

    @Test fun save_changes_to_user_name() {
        val externalId = URI.create("http://www.example.com")
        val userId = UUID.randomUUID()
        val streamId = StreamId("user", userId.toString())
        eventSource.streamWriter.write(streamId, listOf(
                NewEvent("UserExternalIdAssigned", jsonBlob("{ external_id: '$externalId' }"), Blob.empty)
        ))

        val user = repo.findUser(userId)!!
        user.name = "A Suer"
        repo.save(user, emptyMetadata)
        user.name = "A User"
        repo.save(user, emptyMetadata)

        assertThat(eventSource.streamReader.readStreamForwards(streamId).map { it.event }.toListAndClose(),
                contains(eventRecord("UserExternalIdAssigned", "{ external_id: '$externalId' }"),
                        eventRecord("UserNameChanged", "{ new_value: 'A Suer' }"),
                        eventRecord("UserNameChanged", "{ new_value: 'A User' }")))
    }

    @Test fun spurious_changes_not_recorded() {
        val externalId = URI.create("http://www.example.com")
        val userId = UUID.randomUUID()
        val streamId = StreamId("user", userId.toString())
        eventSource.streamWriter.write(streamId, listOf(
                NewEvent("UserExternalIdAssigned", jsonBlob("{ external_id: '$externalId' }"), Blob.empty)
        ))

        val user = repo.findUser(userId)!!
        user.name = "A Suer"
        repo.save(user, emptyMetadata)
        user.name = "A Suer"
        repo.save(user, emptyMetadata)

        assertThat(eventSource.streamReader.readStreamForwards(streamId).map { it.event }.toListAndClose(),
                contains(eventRecord("UserExternalIdAssigned", "{ external_id: '$externalId' }"),
                        eventRecord("UserNameChanged", "{ new_value: 'A Suer' }")))
    }

    fun URI.toUUID(): UUID = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL).generate(toString())

    fun <T> T?.assertNotNull(): T = with(this) { assertNotNull(this); this!! }
}

private fun jsonBlob(text: String): Blob {
    val jsonFactory = JsonFactory()
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    val baos = ByteArrayOutputStream()
    jsonFactory.createParser(text).use { jsonParser ->
        jsonFactory.createGenerator(baos).use { jsonGenerator ->
            jsonParser.nextToken()
            jsonGenerator.copyCurrentStructure(jsonParser)
        }
    }
    return Blob(baos.toByteArray())
}

private fun eventRecord(streamId: StreamId, eventNumber: Long, type: String, data: Matcher<ByteArray>): Matcher<EventRecord> {
    return object : TypeSafeDiagnosingMatcher<EventRecord>() {
        override fun matchesSafely(item: EventRecord, mismatchDescription: Description): Boolean {
            if (item.streamId != streamId) {
                mismatchDescription.appendText("stream was ").appendValue(item.streamId)
                return false
            }
            if (item.type != type) {
                mismatchDescription.appendText("type was ").appendValue(item.type)
                return false
            }
            if (item.eventNumber != eventNumber) {
                mismatchDescription.appendText("event number was ").appendValue(item.eventNumber)
                return false
            }
            val bytes: ByteArray = item.data.read()
            mismatchDescription.appendText("data ")
            data.describeMismatch(bytes, mismatchDescription)
            return data.matches(bytes)
        }

        override fun describeTo(description: Description) {
            description.appendText("event in ").appendValue(streamId)
                    .appendText(", number ").appendValue(eventNumber)
                    .appendText(", type ").appendValue(type)
                    .appendText(", with data ").appendDescriptionOf(data)
        }
    }
}

private fun eventRecord(type: String, jsonData: String) = eventRecord(type, jsonBytesEquivalentTo(jsonData))

private fun eventRecord(type: String, data: Matcher<ByteArray>): Matcher<EventRecord> {
    return object : TypeSafeDiagnosingMatcher<EventRecord>() {
        override fun matchesSafely(item: EventRecord, mismatchDescription: Description): Boolean {
            if (item.type != type) {
                mismatchDescription.appendText("type was ").appendValue(item.type)
                return false
            }
            val bytes: ByteArray = item.data.read()
            mismatchDescription.appendText("data ")
            data.describeMismatch(bytes, mismatchDescription)
            return data.matches(bytes)
        }

        override fun describeTo(description: Description) {
            description.appendText("event of type ").appendValue(type)
                    .appendText(", with data ").appendDescriptionOf(data)
        }
    }
}
