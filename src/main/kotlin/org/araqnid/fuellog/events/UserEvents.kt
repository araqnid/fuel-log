package org.araqnid.fuellog.events

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class UserExternalIdAssigned(@JsonProperty("external_id") val externalId: URI) : Event

data class GoogleProfileData(val givenName: String?, val familyName: String?, val picture: URI?)

data class FacebookProfileData(val picture: URI?)

interface UserDataChangedEvent<out T> : Event {
    val newValue: T?
}

data class UserNameChanged(@JsonProperty("new_value") override val newValue: String?): UserDataChangedEvent<String>
data class GoogleProfileChanged(@JsonProperty("new_value") override val newValue: GoogleProfileData?): UserDataChangedEvent<GoogleProfileData>
data class FacebookProfileChanged(@JsonProperty("new_value") override val newValue: FacebookProfileData?): UserDataChangedEvent<FacebookProfileData>
