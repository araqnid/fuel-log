package org.araqnid.fuellog

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.google.common.util.concurrent.Service
import com.google.common.util.concurrent.ServiceManager
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.ProvisionException
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Names
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.client.HttpAsyncClient
import org.araqnid.appstatus.AppVersion
import org.araqnid.appstatus.ComponentsBuilder
import org.araqnid.appstatus.StatusComponent
import org.araqnid.eventstore.EventSource
import org.araqnid.eventstore.filesystem.TieredFilesystemEventSource
import org.araqnid.eventstore.subscription.PollingEventSubscriptionService
import org.araqnid.eventstore.subscription.SnapshotEventSubscriptionService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Duration
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

class AppConfig(val environment: Map<String, String>) : AbstractModule() {
    val usingSnapshots: Boolean = with (environment["SNAPSHOT_SPOOL"] ?: "") {
        !isEmpty() && Files.isDirectory(Paths.get(this))
    }

    override fun configure() {
        environment.forEach { k, v -> bindConstant().annotatedWith(Names.named(k)).to(v) }

        bind(EventSource::class.java).to(TieredFilesystemEventSource::class.java)
        bind(Clock::class.java).toInstance(Clock.systemDefaultZone())
        bind(InfoResources::class.java)
        bind(RootResource::class.java)
        bind(IdentityResources::class.java)
        bind(FuelResources::class.java)
        bind(LocalUserSecurityFeature::class.java)
        bind(PreferencesResources::class.java)

        with(Multibinder.newSetBinder(binder(), Service::class.java)) {
            addBinding().to(JettyService::class.java)

            if (usingSnapshots) {
                addBinding().to(SnapshotEventSubscriptionService::class.java)
            }
            else {
                addBinding().to(PollingEventSubscriptionService::class.java)
            }
        }

        with(Multibinder.newSetBinder(binder(), Any::class.java, StatusSource::class.java)) {
            addBinding().toInstance(BasicStatusComponents)
            addBinding().to(StatusComponents::class.java)
            addBinding().to(SubscriptionStatistics::class.java)
            if (usingSnapshots) {
                addBinding().to(SnapshotStatistics::class.java)
            }
        }

        bind(HttpAsyncClient::class.java).to(CloseableHttpAsyncClient::class.java)
    }

    @Provides
    fun facebookClientConfig(@Named("FACEBOOK_APP_ID") id: String, @Named("FACEBOOK_APP_SECRET") secret: String) = FacebookClientConfig(id, secret)

    @Provides
    fun googleClientConfig(@Named("GOOGLE_CLIENT_ID") id: String, @Named("GOOGLE_CLIENT_SECRET") secret: String) = GoogleClientConfig(id, secret)

    @Provides
    @Singleton
    fun filesystemEventSource(clock: Clock): TieredFilesystemEventSource {
        return TieredFilesystemEventSource(getenv("EVENT_SPOOL").toPath(), clock)
    }

    @Provides
    @Singleton
    fun subscription(eventSource: EventSource, sink: EventProcessorImpl, readiness: ReadinessListener, statistics: SubscriptionStatistics): PollingEventSubscriptionService {
        return PollingEventSubscriptionService(eventSource.storeReader, sink, Duration.ofSeconds(1)).apply {
            addListener(statistics, directExecutor())
            addListener(readiness, directExecutor())
        }
    }

    @Provides
    @Singleton
    fun snapshotSubscription(clock: Clock, snapshotPersister: EventProcessorImpl, subscription: PollingEventSubscriptionService, statistics: SnapshotStatistics): SnapshotEventSubscriptionService {
        return SnapshotEventSubscriptionService(subscription, snapshotPersister, clock, Duration.ofMinutes(15)).apply {
            addListener(SnapshotSubscriptionLogger(), directExecutor())
            addListener(statistics, directExecutor())
        }
    }

    @Provides
    fun eventStoreReader(source: EventSource) = source.storeReader

    @Provides
    fun eventCategoryReader(source: EventSource) = source.categoryReader

    @Provides
    fun eventStreamReader(source: EventSource) = source.streamReader

    @Provides
    fun eventStreamWriter(source: EventSource) = source.streamWriter

    @Provides
    fun jacksonJsonProvider() = JacksonJsonProvider(jacksonObjectMapper()
            .registerModules(JavaTimeModule(), Jdk8Module(), GuavaModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE))

    @Provides
    @Singleton
    fun serviceManager(services: @JvmSuppressWildcards Set<Service>) = ServiceManager(services)

    @Provides
    @Singleton
    fun appVersion() = AppVersion.fromPackageManifest(javaClass)

    @Provides
    @Singleton
    fun statusComponents(builder: ComponentsBuilder, @StatusSource statusComponentSources: @JvmSuppressWildcards Set<Any>): Collection<StatusComponent> {
        return builder.buildStatusComponents(*statusComponentSources.toTypedArray())
    }

    @Provides
    @Singleton
    fun httpAsyncClient(): CloseableHttpAsyncClient = HttpAsyncClients.createDefault()

    fun String.toPath(): Path = Paths.get(this)

    private fun getenv(key: String): String = environment[key] ?: throw ProvisionException("$key not specified in environment")
}

@Qualifier
annotation class StatusSource

data class FacebookClientConfig(val id: String, val secret: String)

data class GoogleClientConfig(val id: String, val secret: String)
