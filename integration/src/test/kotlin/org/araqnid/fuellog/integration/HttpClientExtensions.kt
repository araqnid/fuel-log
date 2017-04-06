package org.araqnid.fuellog.integration

import com.google.common.io.ByteSource
import com.google.common.io.CharSource
import org.apache.http.HttpEntity
import org.apache.http.HttpRequest
import org.apache.http.entity.ContentType
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

fun <T : HttpRequest> T.accepting(mimeType: String): T = apply {
    addHeader("Accept", mimeType)
}

val HttpEntity.mimeType: String
    get() = ContentType.getOrDefault(this).mimeType

fun HttpEntity.asByteSource(): ByteSource = object : ByteSource() {
    override fun openStream(): InputStream = content
}

fun HttpEntity.asCharSource(charset: Charset = UTF_8): CharSource = asByteSource().asCharSource(charset)

val HttpEntity.bytes: ByteArray
    get() = asByteSource().read()

val HttpEntity.text: String
    get() = asCharSource().read()
