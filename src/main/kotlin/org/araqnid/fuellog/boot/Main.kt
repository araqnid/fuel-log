package org.araqnid.fuellog.boot

import com.google.common.util.concurrent.Service
import com.google.common.util.concurrent.ServiceManager
import com.google.inject.Guice
import com.google.inject.Stage
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.araqnid.fuellog.AppConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic fun main(args: Array<String>) {
        System.setProperty("org.jboss.logging.provider", "slf4j")
        val injector = Guice.createInjector(Stage.PRODUCTION, AppConfig(System.getenv()))

        val serviceManager = injector.getInstance(ServiceManager::class.java)
        serviceManager.addListener(object : ServiceManager.Listener() {
            override fun healthy() {
                logger.info("Services healthy")
            }

            override fun failure(service: Service) {
                logger.error("Service $service has failed, shutting down", service.failureCause())
                System.exit(1)
            }
        })
        val httpAsyncClient = injector.getInstance(CloseableHttpAsyncClient::class.java)

        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            try {
                serviceManager.stopAsync().awaitStopped(10, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                logger.warn("Timeout waiting for services to stop")
            }
            httpAsyncClient.close()
        }, "shutdown"))

        httpAsyncClient.start()
        serviceManager.startAsync()
    }
}
