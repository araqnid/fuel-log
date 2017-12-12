import org.araqnid.gradle.RuntimeDependenciesTask
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

plugins {
    application
    kotlin("jvm") version "1.2.0"
    id("com.timgroup.webpack") version "1.0.1" apply false
}

application {
    mainClassName = "org.araqnid.fuellog.boot.Main"
}

val jettyVersion by extra { "9.4.8.v20171121" }
val jacksonVersion by extra { "2.9.1" }
val resteasyVersion by extra { "3.1.4.Final" }
val guiceVersion by extra { "4.1.0" }
val guavaVersion by extra { "23.5-jre" }

val gitVersion by extra {
    val capture = ByteArrayOutputStream()
    project.exec {
        commandLine("git", "describe", "--tags")
        standardOutput = capture
    }
    String(capture.toByteArray())
            .trim()
            .removePrefix("v")
            .replace('-', '.')
}

allprojects {
    group = "org.araqnid"
    version = gitVersion

    repositories {
        mavenCentral()
        maven(url = "https://repo.araqnid.org/maven/")
        maven(url = "https://dl.bintray.com/araqnid/maven")
    }

    tasks {
        withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
            options.isIncremental = true
            options.isDeprecation = true
        }

        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    afterEvaluate {
        if (tasks.findByName("compileKotlin") != null) {
            kotlin {
                experimental.coroutines = Coroutines.ENABLE
            }
        }
    }
}

configurations {
    "runtime" {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "log4j", module = "log4j")
    }
    "testRuntime" {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
    create("boot")
}

tasks {
    val runtimeDeps by creating(RuntimeDependenciesTask::class)

    "jar"(Jar::class) {
        manifest {
            attributes["Implementation-Title"] = project.description ?: project.name
            attributes["Implementation-Version"] = project.version
            attributes["X-Service-Class"] = application.mainClassName
        }
        from("ui/build/site") {
            into("www")
        }
        from(runtimeDeps) {
            into("META-INF")
        }
        dependsOn(":ui:webpack", runtimeDeps)
    }
}

dependencies {
    compile("org.araqnid:app-status:0.0.12")
    compile("org.araqnid:eventstore:0.0.20")
    compile("com.google.guava:guava:$guavaVersion")
    compile("com.google.inject:guice:$guiceVersion")
    compile("com.google.inject.extensions:guice-servlet:$guiceVersion")
    compile("com.google.inject.extensions:guice-multibindings:$guiceVersion")
    compile("org.slf4j:slf4j-api:1.7.25")
    compile("org.eclipse.jetty:jetty-server:$jettyVersion")
    compile("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    compile("org.jboss.resteasy:resteasy-jaxrs:$resteasyVersion")
    compile("org.jboss.resteasy:resteasy-guice:$resteasyVersion")
    compile("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:$jacksonVersion")
    compile("com.fasterxml.jackson.module:jackson-module-guice:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-guava:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("org.apache.httpcomponents:httpasyncclient:4.1.3")
    compile("com.fasterxml.uuid:java-uuid-generator:3.1.3")
    compile("org.tukaani:xz:1.5")
    compile("org.apache.commons:commons-compress:1.13")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.20")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testCompile(kotlin("test-junit"))
    testCompile(project(":test-utils"))
    testCompile("com.timgroup:clocks-testing:1.0.1070")
    testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:0.20")
    testCompile("org.araqnid:hamkrest-json:1.0.3")
    runtime("ch.qos.logback:logback-classic:1.2.2")
    runtime("org.slf4j:jcl-over-slf4j:1.7.25")
}
