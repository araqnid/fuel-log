package org.araqnid.fuellog.integration

import com.natpryce.hamkrest.isEmptyString
import org.araqnid.fuellog.hamkrest.assumeThat
import kotlin.reflect.KProperty

private fun env(name: String): EnvironmentAccessor {
    return EnvironmentAccessor(name)
}

private val env = EnvironmentAccessor(null)

private class EnvironmentAccessor(private val name: String?) {
    operator fun getValue(source: Nothing?, property: KProperty<*>): String {
        val key = name ?: property.name.toShoutyName()
        val value = System.getenv(key) ?: ""
        assumeThat("$key must be set", value, !isEmptyString)
        return value
    }

    private fun CharSequence.toShoutyName(): String {
        return buildString {
            var insertUnderscore = false
            for (c in this@toShoutyName) {
                if (c.isUpperCase() && insertUnderscore) {
                    append('_')
                    insertUnderscore = false
                }
                append(c.toUpperCase())
                if (!c.isUpperCase()) {
                    insertUnderscore = true
                }
            }
        }
    }
}

val googleClientId by env
val googleClientSecret by env
val googleIdToken by env

val facebookAppId by env
val facebookAppSecret by env
// some tests can be performed only with a current, valid, user access token
val accessToken by env("FACEBOOK_USER_ACCESS_TOKEN")
