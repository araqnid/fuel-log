package org.araqnid.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.task
import java.io.File
import java.security.MessageDigest

open class RuntimeDependenciesTask : DefaultTask() {
    init {
        group = "build"
        description = "produces runtime dependencies files for delivery"
    }

    @get:OutputDirectory
    var outputDir = File(project.buildDir, name)

    @get:InputFiles
    val runtime: Configuration by lazy { project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) }

    @get:InputFiles @get:Optional
    val boot: Configuration? by lazy { project.configurations.findByName("boot") }

    @TaskAction
    fun run() {
        writeFile(runtime, "${project.name}.deps.txt")
        writeFile(boot, "${project.name}.bootdeps.txt")
    }

    private fun writeFile(cfg: Configuration?, filename: String) {
        val sha1 = MessageDigest.getInstance("SHA-1")
        val file = File(outputDir, filename)
        logger.info("writing runtime dependencies to $file")
        file.outputStream().bufferedWriter().use { w ->
            if (cfg == null) return@use
            cfg.resolvedConfiguration.resolvedArtifacts
                    .map { artifact ->
                        val digest = sha1.digest(artifact.file.readBytes()).toHexString()
                        Dep(digest, artifact.moduleVersion.id.toString(), artifact.type)
                    }
                    .sortedBy { dep -> dep.gav }
                    .forEach { dep -> w.write("${dep.digest} ${dep.gav} ${dep.type}\n")}
        }
    }
}

fun ByteArray.toHexString(): String {
    val digits = "0123456789abcdef"
    val chars = CharArray(size * 2)
    var p = 0
    forEach {
        chars[p++] = digits[(it.toInt() and 0xf0) shr 4]
        chars[p++] = digits[it.toInt() and 0x0f]
    }
    return String(chars)
}

fun Project.withRuntimeDependencies() =
        task<RuntimeDependenciesTask>("runtimeDeps")

data class Dep(val digest: String, val gav: String, val type: String)
