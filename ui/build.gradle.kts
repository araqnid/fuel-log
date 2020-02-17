import com.timgroup.gradle.webpack.WebpackTask

plugins {
    id("com.timgroup.webpack")
}

node {
    version = "12.14.1"
    download = true
}

val web by configurations.creating
val appStatus by configurations.creating

dependencies {
    appStatus("org.araqnid.app-status:app-status-core:${LibraryVersions.appStatus}") {
        isTransitive = false
    }
}

tasks.withType(WebpackTask::class).configureEach {
    inputs.files(appStatus.fileCollection { dep -> dep.group == "org.araqnid" && dep.name == "app-status" })

    gzipResources = false
    generateManifest = false

    doFirst {
        val jarFile = appStatus
                .resolvedConfiguration.resolvedArtifacts
                .filter { artifact ->
                    artifact.moduleVersion.id.group == "org.araqnid.app-status" && artifact.moduleVersion.id.name == "app-status-core"
                }
                .map { it.file }
                .single()
        logger.info("Include app-status UI from $jarFile")
        val tmpDir = File(buildDir, "appStatus")
        sync {
            into(tmpDir)
            from(zipTree(jarFile))
            include("org/araqnid/appstatus/site/**")
        }
        sync {
            into(File(buildDir, "site/_status"))
            from(File(tmpDir, "org/araqnid/appstatus/site"))
            exclude("*.gz")
            exclude(".MANIFEST")
        }
    }
}

dependencies {
    val webpack by tasks.named("webpack")
    web(files("$buildDir/site") {
        builtBy(webpack)
    })
}
