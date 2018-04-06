plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation(project(":server"))
    implementation("com.fasterxml.uuid:java-uuid-generator:3.1.3")
}
