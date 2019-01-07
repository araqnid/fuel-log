import org.araqnid.gradle.RuntimeDependenciesTask

val guiceVersion: String by rootProject.extra
val jacksonVersion: String by rootProject.extra
val jettyVersion: String by rootProject.extra
val resteasyVersion: String by rootProject.extra
val kotlinCoroutinesVersion: String by rootProject.extra
val guavaVersion: String by rootProject.extra
val appStatusVersion: String by rootProject.extra

plugins {
    application
    `java-library`
    kotlin("jvm")
}

application {
    mainClassName = "org.araqnid.fuellog.boot.Main"
}

val web by configurations.creating

configurations {
    "runtime" {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "log4j", module = "log4j")
    }
    "testRuntime" {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

tasks {
    val runtimeDeps by registering(RuntimeDependenciesTask::class) {
        appName = "fuel-log"
    }

    "jar"(Jar::class) {
        manifest {
            attributes["Implementation-Title"] = project.description ?: project.name
            attributes["Implementation-Version"] = project.version
            attributes["X-Service-Class"] = application.mainClassName
        }
        from(web) {
            into("www")
            exclude(".cache")
        }
        from(runtimeDeps) {
            into("META-INF")
        }
    }
}

dependencies {
    api("org.araqnid:eventstore:0.0.23")
    api("com.google.inject:guice:$guiceVersion")
    api("com.google.guava:guava:$guavaVersion")
    implementation("org.araqnid:app-status:$appStatusVersion")
    implementation("org.araqnid:kotlin-coroutines-resteasy:1.3.1")
    implementation("com.google.inject.extensions:guice-servlet:$guiceVersion")
    implementation("com.google.inject.extensions:guice-multibindings:$guiceVersion")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.jboss.resteasy:resteasy-jaxrs:$resteasyVersion")
    implementation("org.jboss.resteasy:resteasy-guice:$resteasyVersion")
    implementation("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-guice:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.apache.httpcomponents:httpasyncclient:4.1.3")
    implementation("com.fasterxml.uuid:java-uuid-generator:3.1.3")
    implementation("org.tukaani:xz:1.5")
    implementation("org.apache.commons:commons-compress:1.13")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":test-utils"))
    testImplementation("com.timgroup:clocks-testing:1.0.1070")
    testImplementation("org.araqnid:hamkrest-json:1.0.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.2")
    runtimeOnly("org.slf4j:jcl-over-slf4j:1.7.25")
    web(project(path = ":ui", configuration = "web"))
}
