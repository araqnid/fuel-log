plugins {
    kotlin("jvm") version "1.4.30" apply false
    kotlin("plugin.serialization") version "1.4.30" apply false
    id("com.timgroup.webpack") version "1.0.64" apply false
}

subprojects {
    group = "org.araqnid.fuel-log"

    repositories {
        mavenLocal()
        mavenCentral()
        if (isGithubUserAvailable(project)) {
            for (repo in listOf("assert-that")) {
                maven(url = "https://maven.pkg.github.com/araqnid/$repo") {
                    name = "github-$repo"
                    credentials(githubUserCredentials(project))
                }
            }
        }
        jcenter()
    }
}
