package org.araqnid.fuellog

import com.google.common.util.concurrent.Service
import com.google.common.util.concurrent.ServiceManager
import org.araqnid.appstatus.OnStatusPage
import org.araqnid.appstatus.StatusReport
import org.eclipse.jetty.util.Jetty

class StatusComponents {
    @Suppress("UnstableApiUsage")
    @OnStatusPage
    fun services(serviceManager: ServiceManager) = serviceManager.servicesByState().values()
            .map { service ->
                val priority = if (service.state() === Service.State.FAILED)
                    StatusReport.Priority.CRITICAL
                else
                    StatusReport.Priority.OK
                StatusReport(priority, service.toString())
            }
            .let { reports ->
                StatusReport(
                        reports.map { it.priority }.max() ?: StatusReport.Priority.OK,
                        reports.joinToString("|") { it.text }
                )
            }
}

object BasicStatusComponents {
    @OnStatusPage(label = "JVM version")
    val jvmVersion by lazy {
        val vendorVersion = System.getProperty("java.vendor.version").takeIf { it.isNotBlank() }
        when {
            vendorVersion != null -> "${Runtime.version()} ($vendorVersion)"
            else -> Runtime.version().toString()
        }
    }

    @OnStatusPage("Kotlin version")
    val kotlinVersion = KotlinVersion.CURRENT.toString()

    @OnStatusPage(label = "Jetty version")
    val jettyVersion = Jetty.VERSION!!
}
