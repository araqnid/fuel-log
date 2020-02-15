plugins {
    kotlin("jvm") version "1.3.61" apply false
    id("com.timgroup.webpack") version "1.0.63" apply false
}

LibraryVersions.toMap().forEach { (name, value) ->
    ext["${name}Version"] = value
}

allprojects {
    group = "org.araqnid.fuel-log"

    repositories {
        mavenLocal()
        jcenter()
    }
}
