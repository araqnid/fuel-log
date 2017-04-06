package org.araqnid.fuellog.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.BasicHttpEntity
import java.io.OutputStream

class JacksonEntity(val payload: Any, val objectMapper: ObjectMapper) : BasicHttpEntity() {
    init {
        setContentType("application/json")
    }

    override fun writeTo(output: OutputStream) {
        objectMapper.writeValue(output, payload)
    }
}