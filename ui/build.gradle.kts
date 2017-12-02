plugins {
    id("com.timgroup.webpack")
}

tasks {
    "webpack" {
        doFirst {
            val cfg = rootProject.configurations["compile"].copy()
            cfg.isTransitive = false
            val jarFile = cfg.files { dep -> dep.group == "org.araqnid" && dep.name == "app-status" }.single()
            val tmpDir = File(buildDir, "appStatus")
            copy {
                into(tmpDir)
                from(zipTree(jarFile))
                include("org/araqnid/appstatus/site/**")
            }
            copy {
                into(file("build/site/_status"))
                from(File(tmpDir, "org/araqnid/appstatus/site"))
            }
        }
    }
}