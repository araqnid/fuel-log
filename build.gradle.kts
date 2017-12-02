import org.jetbrains.kotlin.daemon.common.toHexString
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

val jettyVersion by extra { "9.4.7.v20170914" }
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
}

tasks {
    val runtimeDeps by creating {
        val sha1 = MessageDigest.getInstance("SHA-1")
        val outputDir = File(buildDir, "runtimeDeps")

        outputs.dir(outputDir)
        inputs.files(configurations["runtime"])

        doLast {
            data class Dep(val digest: String, val gav: String, val type: String)
            outputDir.mkdirs()

            File(outputDir, "${project.name}.deps.txt").outputStream().bufferedWriter().use { w ->
                configurations["runtime"].resolvedConfiguration.resolvedArtifacts
                        .map { artifact ->
                            val digest = sha1.digest(artifact.file.readBytes()).toHexString()
                            Dep(digest, artifact.moduleVersion.id.toString(), artifact.type)
                        }
                        .sortedBy { dep -> dep.gav }
                        .forEach { dep -> w.write("${dep.digest} ${dep.gav} ${dep.type}\n")}
            }

            File(outputDir, "${project.name}.bootdeps.txt").outputStream().bufferedWriter().use {
                // blank - no extra boot classpath entries
            }
        }
    }

    "jar"(Jar::class) {
        manifest {
            attributes["Implementation-Title"] = project.description ?: project.name
            attributes["Implementation-Version"] = project.version
        }
        from("ui/build/site") {
            into("www")
        }
        from("$buildDir/runtimeDeps") {
            into("META-INF")
        }
        dependsOn(":ui:webpack", runtimeDeps)
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
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testCompile(kotlin("test-junit"))
    testCompile("org.hamcrest:hamcrest-library:1.3")
    testCompile(project(":test-utils"))
    testCompile("com.timgroup:clocks-testing:1.0.1070")
    runtime("ch.qos.logback:logback-classic:1.2.2")
    runtime("org.slf4j:jcl-over-slf4j:1.7.25")
}
