package org.araqnid.fuellog

import org.araqnid.appstatus.OnStatusPage
import org.eclipse.jetty.util.Jetty

object BasicStatusComponents {
    @OnStatusPage(label = "JVM version")
    val jvmVersion = System.getProperty("java.version")!!

    @OnStatusPage(label = "Jetty version")
    val jettyVersion = Jetty.VERSION!!
}
