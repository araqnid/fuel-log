package org.araqnid.fuellog

import kotlinx.coroutines.runBlocking
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.araqnid.kotlin.assertthat.assertThat
import org.araqnid.kotlin.assertthat.equalTo
import org.eclipse.jetty.server.NetworkConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.net.URI
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class FacebookClientTest {
    @get:Rule
    val server = FakeServer()

    @get:Rule
    val client = AsyncHttpClientRule()

    @Volatile
    var debugGetHandler: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit = { req, resp ->
        presentedParams = req.parameterMap.entries.map { (key, values) -> key to values[0] }.toMap()
        resp.contentType = "application/json"
        resp.writer.use { pw ->
            pw.println("""{"user_id":"user","type":"user_token","app_id":"app id","application":"application","expires_at":0,"is_valid":true,"scopes":["email","user"]}""")
        }
    }

    @Volatile
    var oauthAccessTokenGetHandler: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit = { req, resp ->
        presentedParams = req.parameterMap.entries.map { (key, values) -> key to values[0] }.toMap()
        resp.contentType = "application/json"
        resp.writer.use { pw ->
            pw.println("""{"access_token":"the_token","token_type":"the_token_type"}""")
        }
    }

    @Volatile
    var meGetHandler: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit = { req, resp ->
        presentedParams = req.parameterMap.entries.map { (key, values) -> key to values[0] }.toMap()
        resp.contentType = "application/json"
        resp.writer.use { pw ->
            pw.println("""{"name":"The User","id":"xyzzy","picture":{"data":{"width":1,"height":1,"url":"https://example.com/picture.png","is_silhouette":false}}}""")
        }
    }

    @Volatile
    var defaultGetHandler: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit = { req, resp ->
        resp.sendError(404)
    }

    @Volatile
    var presentedParams: Map<String, String>? = null

    @Test
    fun `calls token debug endpoint`() {
        val client = FacebookClient(
            server.graphUri,
            FacebookClientConfig(id = "id", secret = "secret"),
            client.httpClient
        )
        runBlocking {
            val tokenInfo = client.validateUserAccessToken("some_token")
            assertThat(tokenInfo.userId, equalTo("user"))
            assertThat(
                presentedParams, equalTo(
                    mapOf(
                        "input_token" to "some_token",
                        "access_token" to "id|secret"
                    )
                )
            )
        }
    }

    @Test
    fun `fetches app token`() {
        val client = FacebookClient(
            server.graphUri,
            FacebookClientConfig(id = "id", secret = "secret"),
            client.httpClient
        )
        runBlocking {
            val token = client.fetchFacebookAppToken()
            assertThat(token, equalTo("the_token"))
            assertThat(
                presentedParams, equalTo(
                    mapOf(
                        "client_id" to "id",
                        "client_secret" to "secret",
                        "grant_type" to "client_credentials",
                    )
                )
            )
        }
    }

    @Test
    fun `fetches current user profile`() {
        val client = FacebookClient(
            server.graphUri,
            FacebookClientConfig(id = "id", secret = "secret"),
            client.httpClient
        )
        runBlocking {
            val profile = client.fetchUsersOwnProfile("the_access_token")
            assertThat(
                profile, equalTo(
                    FacebookClient.UserIdentity(
                        name = "The User",
                        id = "xyzzy",
                        picture = FacebookClient.Picture(
                            FacebookClient.PictureData(
                                width = 1,
                                height = 1,
                                url = URI("https://example.com/picture.png"),
                                silhouette = false
                            )
                        )
                    )
                )
            )
            assertThat(
                presentedParams, equalTo(
                    mapOf(
                        "access_token" to "the_access_token",
                        "fields" to "id,name,picture",
                    )
                )
            )
        }
    }

    @Test
    fun `fetches other user profile`() {
        val client = FacebookClient(
            server.graphUri,
            FacebookClientConfig(id = "id", secret = "secret"),
            client.httpClient
        )
        defaultGetHandler = meGetHandler
        runBlocking {
            val profile = client.fetchUserProfile("user-x")
            assertThat(
                profile, equalTo(
                    FacebookClient.UserIdentity(
                        name = "The User",
                        id = "xyzzy",
                        picture = FacebookClient.Picture(
                            FacebookClient.PictureData(
                                width = 1,
                                height = 1,
                                url = URI("https://example.com/picture.png"),
                                silhouette = false
                            )
                        )
                    )
                )
            )
            assertThat(
                presentedParams, equalTo(
                    mapOf(
                        "access_token" to "id|secret",
                        "fields" to "id,name,picture",
                    )
                )
            )
        }
    }

    inner class FakeServer : ExternalResource() {
        private val server = Server().apply {
            addConnector(ServerConnector(this))
            handler = ServletContextHandler().apply {
                addServlet(ServletHolder(object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        debugGetHandler(req, resp)
                    }
                }), "/debug_token")
                addServlet(ServletHolder(object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        oauthAccessTokenGetHandler(req, resp)
                    }
                }), "/oauth/access_token")
                addServlet(ServletHolder(object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        meGetHandler(req, resp)
                    }
                }), "/me")
                addServlet(ServletHolder(object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        defaultGetHandler(req, resp)
                    }
                }), "/*")
            }
        }

        val port get() = (server.connectors[0] as NetworkConnector).localPort

        val graphUri get() = URI("http://localhost:${port}/")

        override fun before() {
            server.start()
        }

        override fun after() {
            server.stop()
        }
    }

    class AsyncHttpClientRule : ExternalResource() {
        val httpClient = HttpAsyncClients.createDefault()

        override fun before() {
            httpClient.start()
        }

        override fun after() {
            httpClient.close()
        }
    }
}
