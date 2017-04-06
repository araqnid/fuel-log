package org.araqnid.fuellog

import com.google.common.util.concurrent.AbstractIdleService
import org.eclipse.jetty.security.SecurityHandler
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.Slf4jRequestLog
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.server.session.DefaultSessionIdManager
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.servlet.DispatcherType

@Singleton
class JettyService @Inject constructor(@Named("PORT") port: Int, @Named("DOCUMENT_ROOT") documentRoot: String, resteasyBootstrap: GuiceResteasyBootstrapServletContextListener) : AbstractIdleService() {
    val threadPool = QueuedThreadPool().apply {
        name = "Jetty"
    }
    val server: Server = Server(threadPool).apply {
        addConnector(ServerConnector(this, 1, 1).apply {
            this.port = port
        })
        sessionIdManager = DefaultSessionIdManager(this).apply {
            workerName = null
        }
        requestLog = Slf4jRequestLog().apply {
            logLatency = true
        }
        handler = GzipHandler() wrapping StatisticsHandler() wrapping ContextHandlerCollection().apply {
            val apiContext = ServletContextHandler().apply {
                contextPath = "/_api"
                sessionHandler = SessionHandler().apply {
                    httpOnly = true
                }
                securityHandler = LocalUserSecurityHandler()
                addFilter(FilterHolder(Filter30Dispatcher()), "/*", EnumSet.of(DispatcherType.REQUEST))
                addServlet(DefaultServlet::class.java, "/")
                addEventListener(resteasyBootstrap)
            }
            addHandler(apiContext)
            addContext("/", documentRoot).apply {
                handler = ResourceHandler()
            }
        }
    }

    override fun startUp() {
        server.start()
    }

    override fun shutDown() {
        server.stop()
    }

    private infix fun <T : HandlerWrapper> T.wrapping(handler: Handler): T {
        this.handler = handler
        return this
    }

    private fun <First : HandlerWrapper, Next: Handler> First.andThen(nextHandler: Next): Next {
        handler = nextHandler
        return nextHandler
    }
}
