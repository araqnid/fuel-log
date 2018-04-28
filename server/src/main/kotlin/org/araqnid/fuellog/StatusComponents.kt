package org.araqnid.fuellog

import com.google.common.util.concurrent.Service
import com.google.common.util.concurrent.ServiceManager
import org.araqnid.appstatus.OnStatusPage
import org.araqnid.appstatus.StatusReport
import org.eclipse.jetty.util.Jetty

class StatusComponents {
    @OnStatusPage
    fun services(serviceManager: ServiceManager) = serviceManager.servicesByState().values()
            .map { service ->
                val priority = if (service.state() === Service.State.FAILED)
                    StatusReport.Priority.CRITICAL
                else
                    StatusReport.Priority.OK
                StatusReport(priority, service.toString())
            }
            .reduce { l, r ->
                StatusReport(StatusReport.Priority.higher(l.priority, r.priority),
                        l.text + " | " + r.text)
            }
}

object BasicStatusComponents {
    @OnStatusPage(label = "JVM version")
    val jvmVersion: String by lazy {
        val basicVersion = System.getProperty("java.version")
        when (basicVersion) {
            "10" -> System.getProperty("java.vendor.version")
            else -> basicVersion
        }
    }

    @OnStatusPage("Kotlin version")
    val kotlinVersion = KotlinVersion.CURRENT.toString()

    @OnStatusPage(label = "Jetty version")
    val jettyVersion: String = Jetty.VERSION
}
