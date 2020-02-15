package org.araqnid.fuellog

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ServiceManager
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import com.google.inject.util.Modules
import com.timgroup.clocks.testing.ManualClock
import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.araqnid.eventstore.EventSource
import org.araqnid.eventstore.InMemoryEventSource
import org.araqnid.fuellog.test.NIOTemporaryFolder
import org.eclipse.jetty.server.NetworkConnector
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.URI
import java.time.Clock

class ServerRunner(val environment: Map<String, String> = defaultEnvironment) : ExternalResource() {
    companion object {
        val defaultEnvironment = mapOf(
                "PORT" to "0",
                "FACEBOOK_APP_ID" to "",
                "FACEBOOK_APP_SECRET" to "",
                "GOOGLE_CLIENT_ID" to "",
                "GOOGLE_CLIENT_SECRET" to "",
                "GOOGLE_MAPS_API_KEY" to "xxx"
        )
    }

    lateinit var injector: Injector

    inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}
    inline fun <reified T> instance(): T = injector.getInstance(Key.get(typeLiteral()))
    inline fun <reified T, Ann : Annotation> instance(annotationClass: Class<Ann>): T = injector.getInstance(Key.get(
            typeLiteral(),
            annotationClass))

    inline fun <reified T, Ann : Annotation> instance(annotation: Ann): T = injector.getInstance(Key.get(typeLiteral(),
            annotation))

    val snapshotsFolder = NIOTemporaryFolder()
    val webContentFolder = NIOTemporaryFolder()

    val client: CloseableHttpClient = HttpClients.custom()
            .setRoutePlanner { target, _, _ ->
                val serverHost = HttpHost("localhost", this@ServerRunner.port)
                when (target) {
                    null, serverHost -> HttpRoute(serverHost)
                    else -> throw HttpException("Host is not local server: $target")
                }
            }
            .build()

    val httpContext = HttpClientContext()

    override fun before() {
        System.setProperty("org.jboss.logging.provider", "slf4j")
        val fullEnvironment = environment.toMutableMap()
        fullEnvironment["SNAPSHOT_SPOOL"] = snapshotsFolder.root.toString()
        fullEnvironment["DOCUMENT_ROOT"] = webContentFolder.root.toString()
        injector = Guice.createInjector(Modules.override(AppConfig(fullEnvironment)).with(IntegrationTestModule()))
        instance<CloseableHttpAsyncClient>().start()
        instance<ServiceManager>().startAsync().awaitHealthy()
    }

    override fun after() {
        if (this::injector.isInitialized) {
            instance<ServiceManager>().stopAsync().awaitStopped()
            instance<CloseableHttpAsyncClient>().close()
        }
        client.close()
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return webContentFolder.apply(snapshotsFolder.apply(super.apply(base, description), description), description)
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
