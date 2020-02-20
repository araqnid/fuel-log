import org.araqnid.gradle.RuntimeDependenciesTask

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

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
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

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

dependencies {
    api("org.araqnid.eventstore:eventstore-api:${LibraryVersions.eventstore}")
    api("org.araqnid.eventstore:eventstore-filesystem:${LibraryVersions.eventstore}")
    api("org.araqnid.eventstore:eventstore-subscription:${LibraryVersions.eventstore}")
    api("com.google.inject:guice:${LibraryVersions.guice}")
    api("com.google.guava:guava:${LibraryVersions.guava}")
    implementation("org.araqnid.app-status:app-status-guice:${LibraryVersions.appStatus}")
    implementation("org.araqnid:kotlin-coroutines-resteasy:1.4.3")
    implementation("com.google.inject.extensions:guice-servlet:${LibraryVersions.guice}")
    implementation("com.google.inject.extensions:guice-multibindings:${LibraryVersions.guice}")
    implementation("org.slf4j:slf4j-api:${LibraryVersions.slf4j}")
    implementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
    implementation("org.eclipse.jetty:jetty-servlet:${LibraryVersions.jetty}")
    implementation("org.jboss.resteasy:resteasy-jaxrs:${LibraryVersions.resteasy}")
    implementation("org.jboss.resteasy:resteasy-guice:${LibraryVersions.resteasy}")
    implementation("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:${LibraryVersions.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-guice:${LibraryVersions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:${LibraryVersions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${LibraryVersions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${LibraryVersions.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${LibraryVersions.jackson}")
    implementation("org.apache.httpcomponents:httpasyncclient:4.1.3")
    implementation("com.fasterxml.uuid:java-uuid-generator:3.1.3")
    implementation("org.tukaani:xz:1.5")
    implementation("org.apache.commons:commons-compress:1.13")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibraryVersions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${LibraryVersions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${LibraryVersions.kotlinCoroutines}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":test-utils"))
    testImplementation("com.timgroup:clocks-testing:1.0.1070")
    testImplementation("org.araqnid:hamkrest-json:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibraryVersions.kotlinCoroutines}")
    testRuntimeOnly("org.slf4j:slf4j-simple:${LibraryVersions.slf4j}")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")
    runtimeOnly("org.slf4j:jcl-over-slf4j:${LibraryVersions.slf4j}")
    web(project(path = ":ui", configuration = "web"))
}
