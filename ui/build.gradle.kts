import com.timgroup.gradle.webpack.WebpackTask

plugins {
    id("com.timgroup.webpack")
}

node {
    version = "10.15.0"
    download = true
}

val web by configurations.creating

tasks.withType(WebpackTask::class).configureEach {
    val runtimeClasspath = project(":server").configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)

    inputs.files(runtimeClasspath.fileCollection { dep -> dep.group == "org.araqnid" && dep.name == "app-status" })

    gzipResources = false
    generateManifest = false

    doFirst {
        val jarFile = runtimeClasspath
                .resolvedConfiguration.resolvedArtifacts
                .filter { artifact ->
                    artifact.moduleVersion.id.group == "org.araqnid" && artifact.moduleVersion.id.name == "app-status"
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
