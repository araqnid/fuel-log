plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    api(kotlin("test-junit"))
    api("com.natpryce:hamkrest:1.4.2.2")
    api("com.fasterxml.jackson.core:jackson-databind:${LibraryVersions.jackson}")
    implementation("com.google.guava:guava:${LibraryVersions.guava}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}
