package org.araqnid.fuellog

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.araqnid.eventstore.EventCategoryReader
import org.araqnid.eventstore.EventStreamReader
import org.araqnid.eventstore.EventStreamWriter
import org.araqnid.eventstore.Position
import org.araqnid.eventstore.StreamId
import org.araqnid.eventstore.emptyStreamEventNumber
import org.araqnid.fuellog.events.Event
import org.araqnid.fuellog.events.EventCodecs
import org.araqnid.fuellog.events.FacebookProfileChanged
import org.araqnid.fuellog.events.FacebookProfileData
import org.araqnid.fuellog.events.GoogleProfileChanged
import org.araqnid.fuellog.events.GoogleProfileData
import org.araqnid.fuellog.events.UserExternalIdAssigned
import org.araqnid.fuellog.events.UserNameChanged
import java.net.URI
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.set
import kotlin.reflect.KProperty

class UserRepository @Inject constructor(val categoryReader: EventCategoryReader, val streamReader: EventStreamReader, val streamWriter: EventStreamWriter, val clock: Clock) {
    private val category = "user"
    private var lastPosition: Position = categoryReader.emptyCategoryPosition(category)
    private val externalIdLookup: MutableMap<URI, UUID> = mutableMapOf()
    private val knownUsers: MutableSet<UUID> = mutableSetOf()

    @Synchronized
    fun sync() {
        runBlocking {
            categoryReader.readCategoryForwards(category, lastPosition)
                .map { Partial(it.position, it.event.streamId.id, EventCodecs.decode(it.event)) }
                .collect { (position, streamId, event) ->
                    val userId = UUID.fromString(streamId)
                    knownUsers.add(userId)
                    if (event is UserExternalIdAssigned)
                        externalIdLookup[event.externalId] = userId
                    lastPosition = position
                }
        }
    }

    operator fun get(userId: UUID): UserRecord {
        val user = UserRecord(userId)
        runBlocking {
            streamReader.readStreamForwards(StreamId("user", userId.toString()))
                .map { Pair(EventCodecs.decode(it.event), it.event.eventNumber) }
                .collect { (event, eventNumber) ->
                    user.accept(event)
                    user.version = eventNumber
                }
        }
        user.unsyncedChanges.clear()
        return user
    }

    fun findUser(userId: UUID): UserRecord? {
        sync()
        if (!knownUsers.contains(userId)) return null
        return get(userId)
    }

    fun findUserByExternalId(externalId: URI): UserRecord? {
        sync()
        val userId = externalIdLookup[externalId]
        if (userId != null)
            return findUser(userId)
        else
            return null
    }

    fun findOrCreateUserByExternalId(externalId: URI, metadata: RequestMetadata): UserRecord {
        val existing = findUserByExternalId(externalId)
        if (existing != null) return existing
        val uuid = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL).generate(externalId.toString())!!
        val registrationEvent = EventCodecs.encode(UserExternalIdAssigned(externalId), metadata)
        val streamId = StreamId("user", uuid.toString())
        streamWriter.write(streamId, listOf(registrationEvent))
        return get(uuid)
    }

    fun save(user: UserRecord, metadata: RequestMetadata) {
        streamWriter.write(user.streamId, user.unsyncedChanges.map { EventCodecs.encode(it, metadata) })
        user.unsyncedChanges.clear()
    }

    private data class Partial(val position: Position, val streamId: String, val event: Event)
}

class UserRecord(val userId: UUID) {
    enum class Realm { TEST, FACEBOOK, GOOGLE }

    var externalId: URI? by RecordChanges { newValue -> UserExternalIdAssigned(newValue!!) }
    var name: String? by RecordChanges(::UserNameChanged)
    var googleProfileData: GoogleProfileData? by RecordChanges(::GoogleProfileChanged)
    var facebookProfileData: FacebookProfileData? by RecordChanges(::FacebookProfileChanged)
    val realm: Realm
        get() = when {
            externalId.toString().startsWith("https://fuel.araqnid.org/_api/user/identity/google/") -> Realm.GOOGLE
            externalId.toString().startsWith("https://fuel.araqnid.org/_api/user/identity/facebook/") -> Realm.FACEBOOK
            externalId.toString().startsWith("https://fuel.araqnid.org/_api/user/identity/test/") -> Realm.TEST
            else -> throw IllegalStateException("Unhandled externalId: $externalId")
        }
    val picture: URI?
        get() = when (realm) {
            UserRecord.Realm.GOOGLE -> googleProfileData?.picture
            UserRecord.Realm.FACEBOOK -> facebookProfileData?.picture
            else -> null
        }

    internal val unsyncedChanges = ArrayList<Event>()
    internal var version = emptyStreamEventNumber

    val streamId = StreamId("user", userId.toString())

    fun accept(event: Event) {
        when (event) {
            is UserExternalIdAssigned -> externalId = event.externalId
            is UserNameChanged -> name = event.newValue
            is GoogleProfileChanged -> googleProfileData = event.newValue
            is FacebookProfileChanged -> facebookProfileData = event.newValue
            else -> throw IllegalArgumentException("Unhandled user event: $event")
        }
    }
}

class RecordChanges<T>(val changeEvent: (T?) -> Event) {
    var currentValue: T? = null

    operator fun getValue(userRecord: UserRecord, property: KProperty<*>): T? {
        return currentValue
    }

    operator fun setValue(userRecord: UserRecord, property: KProperty<*>, newValue: T?) {
        if (currentValue != newValue) {
            userRecord.unsyncedChanges += changeEvent(newValue)
        }
        currentValue = newValue
    }
}
