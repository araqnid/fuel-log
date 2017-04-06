package org.araqnid.fuellog.events

import java.net.URI

data class UserExternalIdAssigned(val externalId: URI) : Event

data class GoogleProfileData(val givenName: String?, val familyName: String?, val picture: URI?)

data class FacebookProfileData(val picture: URI?)

interface UserDataChangedEvent<out T> : Event {
    val newValue: T?
}

data class UserNameChanged(override val newValue: String?): UserDataChangedEvent<String>
data class GoogleProfileChanged(override val newValue: GoogleProfileData?): UserDataChangedEvent<GoogleProfileData>
data class FacebookProfileChanged(override val newValue: FacebookProfileData?): UserDataChangedEvent<FacebookProfileData>
