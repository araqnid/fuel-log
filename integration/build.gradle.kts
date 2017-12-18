plugins {
    kotlin("jvm")
}

configurations {
    "testRuntime" {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

val jettyVersion: String by rootProject.extra
val resteasyVersion: String by rootProject.extra

dependencies {
    testImplementation(rootProject)
    testImplementation(project(":test-utils"))
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation("com.timgroup:clocks-testing:1.0.1070")
    testImplementation("org.araqnid:hamkrest-json:1.0.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.20")
    testImplementation("org.apache.httpcomponents:httpasyncclient:4.1.3")
    testImplementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    testImplementation("org.jboss.resteasy:resteasy-jaxrs:$resteasyVersion")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.25")
}
