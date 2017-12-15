import com.timgroup.gradle.webpack.WebpackTask

plugins {
    id("com.timgroup.webpack")
}

tasks {
    "webpack"(WebpackTask::class) {
        inputs.files(rootProject.configurations["compile"].fileCollection { dep -> dep.group == "org.araqnid" && dep.name == "app-status" })

        gzipResources = false
        generateManifest = false

        doFirst {
            val cfg = rootProject.configurations["compile"].copy()
            cfg.isTransitive = false
            val jarFile = cfg.files { dep -> dep.group == "org.araqnid" && dep.name == "app-status" }.single()
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
            }
        }
    }
}