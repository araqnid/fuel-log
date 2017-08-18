package org.araqnid.fuellog

import org.araqnid.fuellog.events.EventMetadata
import javax.servlet.http.HttpServletRequest

data class RequestMetadata(val clientIp: String, val userAgent: String?) : EventMetadata {
    companion object {
        fun fromServletRequest(servletRequest: HttpServletRequest): RequestMetadata {
            return RequestMetadata(servletRequest.remoteAddr,
                    servletRequest.getHeader("User-Agent"))
        }
    }
}
