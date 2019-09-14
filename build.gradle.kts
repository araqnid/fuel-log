plugins {
    kotlin("jvm") version "1.3.50" apply false
    id("com.timgroup.webpack") version "1.0.50" apply false
}

LibraryVersions.toMap().forEach { (name, value) ->
    ext["${name}Version"] = value
}

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

