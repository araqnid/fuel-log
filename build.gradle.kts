plugins {
    kotlin("jvm") version "1.3.11" apply false
    id("com.timgroup.webpack") version "1.0.50" apply false
}

val jettyVersion by extra("9.4.12.v20180830")
val jacksonVersion by extra("2.9.7")
val resteasyVersion by extra("3.1.4.Final")
val guiceVersion by extra("4.2.1")
val guavaVersion by extra("27.0-jre")
val kotlinCoroutinesVersion by extra("1.0.1")

allprojects {
    group = "org.araqnid.fuel-log"

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

