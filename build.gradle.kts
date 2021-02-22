plugins {
    kotlin("jvm") version "1.4.30" apply false
    id("com.timgroup.webpack") version "1.0.64" apply false
}

allprojects {
    group = "org.araqnid.fuel-log"

    repositories {
        mavenLocal()
        jcenter()
    }
}
