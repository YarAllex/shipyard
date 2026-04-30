package dev.yarallex.shipyard

import dev.yarallex.shipyard.docker.ImageRef
import dev.yarallex.shipyard.git.GitOps
import dev.yarallex.shipyard.log.ShipyardLog
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class ReleaseSummaryTask : DefaultTask() {

    @get:Input
    abstract val imageRepo: Property<String>

    @get:Input
    abstract val registryHost: Property<String>

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val gitBin: Property<String>

    @get:Input
    abstract val gitRemote: Property<String>

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
        val git = GitOps(execOps, layout.projectDirectory.asFile, gitBin.get())
        val tag = git.latestVersionTag(tagPrefix.get())
            ?: throw GradleException("No release tag found after the release pipeline.")
        val version = tag.removePrefix(tagPrefix.get())
        val versionedRef = "$qualified:$version"
        val latestRef = "$qualified:latest"

        log.line()
        log.divider()
        log.header(" Release complete")
        log.divider()
        log.line("  Tag:    $tag")
        log.line("  Image:  $versionedRef")
        log.line("  Latest: $latestRef")
        log.line("  Remote: ${gitRemote.get()}")
        log.divider()
    }
}
