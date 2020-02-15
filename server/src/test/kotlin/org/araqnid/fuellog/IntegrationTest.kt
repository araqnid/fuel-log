package org.araqnid.fuellog

import com.google.common.io.ByteStreams
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import java.util.UUID

fun ServerRunner.execute(request: HttpUriRequest): HttpResponse {
    return client.execute(request, { rawResponse ->
        val bufferedResponse = BasicHttpResponse(rawResponse.statusLine)
        rawResponse.allHeaders.forEach { bufferedResponse.addHeader(it) }
        if (rawResponse.entity != null) {
            val bytes = ByteStreams.toByteArray(rawResponse.entity.content)
            bufferedResponse.entity = ByteArrayEntity(bytes).apply {
                contentType = rawResponse.entity.contentType
            }
        }
        bufferedResponse
    }, httpContext)
}

var HttpContext.currentUser: IdentityResources.UserInfo
    get() = getAttribute("org.araqnid.fuellog.IntegrationTest.currentUser") as IdentityResources.UserInfo
    set(value) {
        setAttribute("org.araqnid.fuellog.IntegrationTest.currentUser", value)
    }

var ServerRunner.currentUser: IdentityResources.UserInfo
    get() = httpContext.currentUser
    set(value) {
        httpContext.currentUser = value
    }

fun ServerRunner.loginAsNewUser() {
    val userName = "Test User"
    val response = execute(HttpPost("/_api/user/identity/test").apply {
        entity = formEntity(mapOf("identifier" to UUID.randomUUID().toString(), "name" to userName))
    })
    httpContext.currentUser = response.readJson()
}
