package org.araqnid.fuellog

import kotlinx.coroutines.runBlocking
import org.araqnid.fuellog.test.assertThrows
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
import java.net.http.HttpClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.BadRequestException

class GoogleClientTest {
    @get:Rule
    val server = FakeServer()

    @Volatile
    var postHandler: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit = { req, resp ->
        presentedToken = req.getParameter("id_token")
        resp.contentType = "application/json"
        resp.writer.use { pw ->
            pw.println("""{"name":"xyzzy","iat":1614118787,"exp":1614118796,"aud":"client-id","sub":"subject","iss":"$tokenIssuer","something_unexpected":0}""")
        }
    }

    @Volatile
    var tokenIssuer = "https://accounts.google.com"

    @Volatile
    var presentedToken: String? = null

    @Test
    fun `calls token validation endpoint`() {
        val client = GoogleClient(
            server.uri,
            GoogleClientConfig(id = "client-id", secret = "client-secret"),
            HttpClient.newHttpClient(),
            Clock.fixed(Instant.ofEpochSecond(1614118780L, 0L), ZoneId.systemDefault())
        )
        runBlocking {
            val tokenInfo = client.validateToken("tokenContent")
            assertThat(tokenInfo.name, equalTo("xyzzy"))
            assertThat(presentedToken, equalTo("tokenContent"))
        }
    }

    @Test
    fun `rejects token with wrong audience`() {
        val client = GoogleClient(
            server.uri,
            GoogleClientConfig(id = "different-client-id", secret = "client-secret"),
            HttpClient.newHttpClient(),
            Clock.fixed(Instant.ofEpochSecond(1614118780L, 0L), ZoneId.systemDefault())
        )
        assertThrows<BadRequestException> {
            runBlocking {
                client.validateToken("tokenContent")
            }
        }
    }

    @Test
    fun `rejects expired token`() {
        val client = GoogleClient(
            server.uri,
            GoogleClientConfig(id = "client-id", secret = "client-secret"),
            HttpClient.newHttpClient(),
            Clock.fixed(Instant.ofEpochSecond(1614118800L, 0L), ZoneId.systemDefault())
        )
        assertThrows<BadRequestException> {
            runBlocking {
                client.validateToken("tokenContent")
            }
        }
    }

    @Test
    fun `rejects token with wrong issuer`() {
        val client = GoogleClient(
            server.uri,
            GoogleClientConfig(id = "client-id", secret = "client-secret"),
            HttpClient.newHttpClient(),
            Clock.fixed(Instant.ofEpochSecond(1614118780L, 0L), ZoneId.systemDefault())
        )
        tokenIssuer = "https://example.com"
        assertThrows<BadRequestException> {
            runBlocking {
                client.validateToken("tokenContent")
            }
        }
    }

    @Test
    fun `propagates HTTP error`() {
        val client = GoogleClient(
            server.uri,
            GoogleClientConfig(id = "client-id", secret = "client-secret"),
            HttpClient.newHttpClient(),
            Clock.fixed(Instant.ofEpochSecond(1614118780L, 0L), ZoneId.systemDefault())
        )
        postHandler = { req, resp -> resp.sendError(404) }
        assertThrows<BadRequestException> {
            runBlocking {
                client.validateToken("tokenContent")
            }
        }
    }

    inner class FakeServer : ExternalResource() {
        private val server = Server().apply {
            addConnector(ServerConnector(this))
            handler = ServletContextHandler().apply {
                addServlet(ServletHolder(object : HttpServlet() {
                    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
                        postHandler(req, resp)
                    }
                }), "/")
            }
        }

        val port get() = (server.connectors[0] as NetworkConnector).localPort

        val uri get() = URI("http://localhost:${port}")

        override fun before() {
            server.start()
        }

        override fun after() {
            server.stop()
        }
    }
}
