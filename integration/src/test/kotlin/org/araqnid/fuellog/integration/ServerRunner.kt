package org.araqnid.fuellog.integration

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ServiceManager
import com.google.inject.*
import com.google.inject.util.Modules
import com.timgroup.clocks.testing.ManualClock
import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.araqnid.eventstore.EventSource
import org.araqnid.eventstore.InMemoryEventSource
import org.araqnid.fuellog.AppConfig
import org.araqnid.fuellog.JettyService
import org.eclipse.jetty.server.NetworkConnector
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.URI
import java.time.Clock

class ServerRunner(val environment: Map<String, String>) : ExternalResource() {
    private var serverInjector: Injector? = null

    val injector: Injector
        get() = serverInjector!!

    inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() { }
    inline fun <reified T> instance(): T = injector.getInstance(Key.get(typeLiteral()))
    inline fun <reified T, Ann : Annotation> instance(annotationClass: Class<Ann>): T = injector.getInstance(Key.get(typeLiteral(), annotationClass))
    inline fun <reified T, Ann : Annotation> instance(annotation: Ann): T = injector.getInstance(Key.get(typeLiteral(), annotation))

    val temporaryFolder = TemporaryFolder()

    val client: CloseableHttpClient = HttpClients.custom()
            .setRoutePlanner({ target, _, _ ->
                val serverHost = HttpHost("localhost", this@ServerRunner.port)
                when (target) {
                    null, serverHost -> HttpRoute(serverHost)
                    else -> throw HttpException("Host is not local server: $target")
                }
            })
            .build()

    override fun before() {
        System.setProperty("org.jboss.logging.provider", "slf4j")
        val fullEnvironment = HashMap(environment)
        fullEnvironment["SNAPSHOT_SPOOL"] = temporaryFolder.newFolder("snapshots").toString()
        serverInjector = Guice.createInjector(Modules.override(AppConfig(fullEnvironment)).with(IntegrationTestModule()))
        instance<ServiceManager>().startAsync().awaitHealthy()
    }

    override fun after() {
        if (serverInjector != null)
            instance<ServiceManager>().stopAsync().awaitStopped()
        client.close()
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return temporaryFolder.apply(super.apply(base, description), description)
    }

    val port: Int
        get() = (instance<JettyService>().server.connectors[0] as NetworkConnector).localPort

    val clock = ManualClock.initiallyAt(Clock.systemDefaultZone())

    fun uri(path: String): URI {
        Preconditions.checkArgument(path.startsWith("/"))
        return URI.create("http://localhost:$port$path")
    }

    private inner class IntegrationTestModule : AbstractModule() {
        override fun configure() {
            bind(EventSource::class.java).to(InMemoryEventSource::class.java)
            bind(Clock::class.java).toInstance(clock)
        }

        @Provides
        @Singleton
        fun inMemoryEventSource(clock: Clock) = InMemoryEventSource(clock)
    }
}
