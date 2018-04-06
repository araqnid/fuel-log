package org.araqnid.fuellog

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

internal val HttpServletRequest.maybeSession: HttpSession?
    get() = this.getSession(false)

internal class HttpSessionAttribute<T : Any>(private val name: String) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(session: HttpSession, property: KProperty<*>): T? =
            property.returnType.jvmErasure.java.cast(session.getAttribute(name)) as T?

    operator fun setValue(session: HttpSession, property: KProperty<*>, value: T?) {
        if (value == null)
            session.removeAttribute(name)
        else
            session.setAttribute(name, value)
    }
}
