package org.araqnid.fuellog

import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

internal val userSessionAttributeName = IdentityResources::class.java.name + ".USER_ID"

internal val HttpServletRequest.maybeSession: HttpSession?
    get() = this.getSession(false)

internal var HttpSession.userId: UUID?
    get() = getAttribute(userSessionAttributeName) as UUID?
    set(value) {
        setAttribute(userSessionAttributeName, value)
    }