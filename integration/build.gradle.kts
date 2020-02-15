plugins {
    `java-library`
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

configurations {
    "testRuntime" {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

dependencies {
    testImplementation(project(":server"))
    testImplementation(project(":test-utils"))
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))
    testImplementation("com.timgroup:clocks-testing:1.0.1070")
    testImplementation("org.araqnid:hamkrest-json:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.apache.httpcomponents:httpasyncclient:4.1.3")
    testImplementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
    testImplementation("org.jboss.resteasy:resteasy-jaxrs:${LibraryVersions.resteasy}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:${LibraryVersions.jackson}")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${LibraryVersions.jackson}")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${LibraryVersions.jackson}")
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))
    testRuntimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
}
