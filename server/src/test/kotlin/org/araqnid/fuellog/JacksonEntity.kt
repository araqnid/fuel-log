package org.araqnid.fuellog

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.BasicHttpEntity
import java.io.OutputStream

class JacksonEntity(val payload: Any, val objectMapper: ObjectMapper = defaultObjectMapper) : BasicHttpEntity() {
    init {
        setContentType("application/json")
    }

    override fun writeTo(output: OutputStream) {
        objectMapper.writeValue(output, payload)
    }
}
