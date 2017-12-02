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
    testCompile("org.hamcrest:hamcrest-library:1.3")
    testCompile("com.timgroup:clocks-testing:1.0.1070")
    testCompile(kotlin("stdlib-jdk8"))
    testRuntime("org.slf4j:slf4j-simple:1.7.25")
}
