package org.araqnid.fuellog

import java.util.*
import javax.servlet.http.HttpSession

var HttpSession.userId: UUID? by HttpSessionAttribute(IdentityResources::class.java.name + ".USER_ID")
