plugins {
    kotlin("jvm")
}

configurations {
    "testRuntime" {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

dependencies {
    testCompile(rootProject)
    testCompile(project(":test-utils"))
    testCompile(kotlin("test-junit"))
    testCompile("com.timgroup:clocks-testing:1.0.1070")
    testCompile(kotlin("stdlib-jdk8"))
    testCompile("org.hamcrest:hamcrest-library:1.3")
    testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:0.20")
    testCompile("org.araqnid:hamkrest-json:1.0.2")
    testRuntime("org.slf4j:slf4j-simple:1.7.25")
}
