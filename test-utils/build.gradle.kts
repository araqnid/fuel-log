plugins {
    `java-library`
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

dependencies {
    api(kotlin("test-junit"))
    api("com.natpryce:hamkrest:1.7.0.0")
    api("com.fasterxml.jackson.core:jackson-databind:${LibraryVersions.jackson}")
    implementation("com.google.guava:guava:${LibraryVersions.guava}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
