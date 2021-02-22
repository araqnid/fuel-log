plugins {
    kotlin("jvm") version "1.4.30" apply false
    kotlin("plugin.serialization") version "1.4.30" apply false
    id("com.timgroup.webpack") version "1.0.64" apply false
}

subprojects {
    group = "org.araqnid.fuel-log"

    repositories {
        mavenLocal()
        jcenter()
    }
}
