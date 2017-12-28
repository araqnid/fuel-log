import org.araqnid.gradle.RuntimeDependenciesTask
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

plugins {
    application
    kotlin("jvm") version "1.2.10"
    id("com.timgroup.webpack") version "1.0.12" apply false
}

application {
    mainClassName = "org.araqnid.fuellog.boot.Main"
}

val jettyVersion by extra("9.4.8.v20171121")
val jacksonVersion by extra("2.9.3")
val resteasyVersion by extra("3.1.4.Final")
val guiceVersion by extra("4.1.0")
val guavaVersion by extra("23.6-jre")

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
        jcenter()
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
            exclude(".cache")
        }
        from(runtimeDeps) {
            into("META-INF")
        }
        dependsOn(":ui:webpack")
    }
}

dependencies {
    compile("org.araqnid:eventstore:0.0.22")
    compile("com.google.inject:guice:$guiceVersion")
    compile("com.google.guava:guava:$guavaVersion")
    implementation("org.araqnid:app-status:0.0.13")
    implementation("org.araqnid:kotlin-coroutines-resteasy:1.0.1")
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.21")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":test-utils"))
    testImplementation("com.timgroup:clocks-testing:1.0.1070")
    testImplementation("org.araqnid:hamkrest-json:1.0.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.2")
    runtimeOnly("org.slf4j:jcl-over-slf4j:1.7.25")
}
