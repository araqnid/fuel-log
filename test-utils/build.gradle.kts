plugins {
    kotlin("jvm")
}

val guavaVersion: String by rootProject.extra
val jacksonVersion: String by rootProject.extra

dependencies {
    compile(kotlin("test-junit"))
    compile("org.hamcrest:hamcrest-library:1.3")
    compile("com.google.guava:guava:$guavaVersion")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation(kotlin("stdlib-jdk8"))
}
