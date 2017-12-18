import com.timgroup.gradle.webpack.WebpackTask
import org.araqnid.gradle.Dep
import org.araqnid.gradle.toHexString

plugins {
    id("com.timgroup.webpack")
}

tasks {
    "webpack"(WebpackTask::class) {
        inputs.files(rootProject.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).fileCollection { dep -> dep.group == "org.araqnid" && dep.name == "app-status" })

        gzipResources = false
        generateManifest = false

        doFirst {
            val jarFile = rootProject.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
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
}