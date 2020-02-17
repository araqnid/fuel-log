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
        when (val basicVersion = System.getProperty("java.version")!!) {
            "10", "11", "12", "13" -> System.getProperty("java.vendor.version")!!
            else -> basicVersion
        }
    }

    @OnStatusPage("Kotlin version")
    val kotlinVersion = KotlinVersion.CURRENT.toString()

    @OnStatusPage(label = "Jetty version")
    val jettyVersion = Jetty.VERSION!!
}
