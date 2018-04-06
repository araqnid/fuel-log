package org.araqnid.fuellog

import java.util.UUID
import javax.servlet.http.HttpSession

var HttpSession.userId: UUID? by HttpSessionAttribute(IdentityResources::class.java.name + ".USER_ID")
