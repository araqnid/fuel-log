plugins {
    `java-library`
    kotlin("jvm")
}

val guavaVersion: String by rootProject.extra
val jacksonVersion: String by rootProject.extra

dependencies {
    api(kotlin("test-junit"))
    api("com.natpryce:hamkrest:1.4.2.2")
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}
