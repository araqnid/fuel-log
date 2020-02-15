package org.araqnid.fuellog.integration

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.client.HttpAsyncClient
import org.junit.rules.ExternalResource

class HttpAsyncClientRule(
        factory: () -> CloseableHttpAsyncClient = HttpAsyncClients::createDefault,
        private val closeableHttpAsyncClient: CloseableHttpAsyncClient = factory()
) : ExternalResource(), HttpAsyncClient by closeableHttpAsyncClient {

    override fun before() {
        closeableHttpAsyncClient.start()
    }

    override fun after() {
        closeableHttpAsyncClient.close()
    }
}
