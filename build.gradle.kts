import org.araqnid.gradle.RuntimeDependenciesTask
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

plugins {
    kotlin("jvm") version "1.3.0" apply false
    id("com.timgroup.webpack") version "1.0.37" apply false
}

val jettyVersion by extra("9.4.12.v20180830")
val jacksonVersion by extra("2.9.7")
val resteasyVersion by extra("3.1.4.Final")
val guiceVersion by extra("4.2.1")
val guavaVersion by extra("27.0-jre")
val kotlinCoroutinesVersion by extra("1.0.1")

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
        mavenLocal()
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
}

