plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation(rootProject)
    implementation("com.fasterxml.uuid:java-uuid-generator:3.1.3")
}
