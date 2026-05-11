package dev.yarallex.shipyard.docker

import dev.yarallex.shipyard.exec.BinaryResolver
import dev.yarallex.shipyard.git.GitOps
import dev.yarallex.shipyard.log.ShipyardLog
import dev.yarallex.shipyard.version.SemVer
import dev.yarallex.shipyard.version.VersionResolver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class DockerBuildTask : DefaultTask() {

    @get:Input
    abstract val imageRepo: Property<String>

    @get:Input
    abstract val registryHost: Property<String>

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val initialVersion: Property<String>

    @get:Input
    abstract val dockerBin: Property<String>

    @get:Input
    abstract val gitBin: Property<String>

    @get:Input
    abstract val platforms: ListProperty<String>

    @get:Input
    abstract val push: Property<Boolean>

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val styledOutputFactory: StyledTextOutputFactory

    @TaskAction
    fun run() {
        val log = ShipyardLog(styledOutputFactory)
        val repo = imageRepo.orNull
            ?: throw GradleException("shipyard.imageRepo is not configured.")
        val qualified = ImageRef.qualify(repo, registryHost.getOrElse(""))
        val workDir = layout.projectDirectory.asFile
        val version = resolveVersion(workDir)
        val versionedRef = "$qualified:$version"
        val latestRef = "$qualified:latest"

        val docker = BinaryResolver.resolve(dockerBin.get())
        val platformList = platforms.getOrElse(emptyList()).filter { it.isNotBlank() }
        val shouldPush = push.getOrElse(false)

        if (!shouldPush && platformList.size > 1) {
            throw GradleException(
                "Multi-platform build (${platformList.joinToString(",")}) requires push=true; " +
                    "buildx --load only supports a single platform. " +
                    "Either set shipyard.platforms to a single value or run a push task.",
            )
        }

        val command = buildList {
            add(docker)
            add("buildx")
            add("build")
            if (platformList.isNotEmpty()) {
                add("--platform")
                add(platformList.joinToString(","))
            }
            when {
                shouldPush -> add("--push")
                platformList.size == 1 -> add("--load")
            }
            add("-t")
            add(versionedRef)
            add("-t")
            add(latestRef)
            add(".")
        }
        val mode = if (shouldPush) "build+push" else "build"
        val platformsDesc = if (platformList.isEmpty()) "default" else platformList.joinToString(",")
        log.arrow("$mode $versionedRef ($platformsDesc)")
        execOps.exec { spec ->
            spec.workingDir(workDir)
            spec.commandLine(command)
            spec.environment("PATH", BinaryResolver.augmentedPath())
        }
        if (shouldPush) {
            log.ok("Pushed: $versionedRef")
            log.ok("Pushed: $latestRef")
        } else {
            log.ok("Built: $versionedRef")
            log.ok("Built: $latestRef")
        }
    }

    private fun resolveVersion(workDir: java.io.File): String {
        val git = GitOps(execOps, workDir, gitBin.get())
        val tag = git.latestVersionTag(tagPrefix.get())
        if (tag != null) {
            val v = SemVer.parseOrNull(tag.removePrefix(tagPrefix.get()))
            if (v != null) return v.toString()
        }
        return VersionResolver.resolve(
            execOps,
            workDir,
            gitBin.get(),
            tagPrefix.get(),
            initialVersion.get(),
        ).next.toString()
    }
}
