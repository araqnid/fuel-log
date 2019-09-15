package org.araqnid.fuellog

import com.google.common.util.concurrent.AbstractIdleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import org.eclipse.jetty.server.CustomRequestLog
import org.eclipse.jetty.server.CustomRequestLog.EXTENDED_NCSA_FORMAT
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.Slf4jRequestLogWriter
import org.eclipse.jetty.server.handler.ContextHandler
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
import kotlin.coroutines.CoroutineContext

@Singleton
class JettyService @Inject constructor(@Named("PORT") port: Int,
                                       @Named("DOCUMENT_ROOT") documentRoot: String,
                                       resteasyBootstrap: GuiceResteasyBootstrapServletContextListener) : AbstractIdleService(), CoroutineScope {
    private val threadPool = QueuedThreadPool().apply {
        name = "Jetty"
    }

    val job = Job()

    val server: Server = Server(threadPool).apply {
        addConnector(ServerConnector(this, 1, 1).apply {
            this.port = port
        })
        sessionIdManager = DefaultSessionIdManager(this).apply {
            workerName = null
        }
        requestLog = CustomRequestLog(Slf4jRequestLogWriter(), EXTENDED_NCSA_FORMAT)
        handler = StatisticsHandler() wrapping ContextHandlerCollection().apply {
            addHandler(ServletContextHandler().apply {
                contextPath = "/_api"
                sessionHandler = SessionHandler().apply {
                    httpOnly = true
                }
                securityHandler = LocalUserSecurityHandler()
                addFilter(FilterHolder(Filter30Dispatcher()), "/*", EnumSet.of(DispatcherType.REQUEST))
                addServlet(DefaultServlet::class.java, "/")
                addEventListener(resteasyBootstrap)
                gzipHandler = GzipHandler()
            })
            addHandler(ContextHandler().apply {
                contextPath = "/"
                resourceBase = documentRoot
                handler = ResourceHandler()
            })
        }
    }

    override val coroutineContext: CoroutineContext
        get() = server.threadPool.asCoroutineDispatcher() + job

    override fun startUp() {
        server.start()
    }

    override fun shutDown() {
        job.cancel()
        server.stop()
    }

    private infix fun <T : HandlerWrapper> T.wrapping(handler: Handler): T {
        this.handler = handler
        return this
    }
}
