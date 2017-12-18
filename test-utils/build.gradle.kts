plugins {
    kotlin("jvm")
}

val guavaVersion: String by rootProject.extra
val jacksonVersion: String by rootProject.extra

dependencies {
    compile(kotlin("test-junit"))
    compile("com.natpryce:hamkrest:1.4.2.2")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}
