package dev.yarallex.shipyard.git

import dev.yarallex.shipyard.log.ShipyardLog
import dev.yarallex.shipyard.version.VersionResolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class PushTagTask : DefaultTask() {

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val initialVersion: Property<String>

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
        val workDir = layout.projectDirectory.asFile
        val git = GitOps(execOps, workDir, gitBin.get())
        val tag = git.latestVersionTag(tagPrefix.get())
            ?: VersionResolver.resolve(
                execOps,
                workDir,
                gitBin.get(),
                tagPrefix.get(),
                initialVersion.get(),
            ).nextTag

        log.arrow("Pushing tag $tag to ${gitRemote.get()}")
        git.pushTag(gitRemote.get(), tag)
        log.ok("Pushed tag $tag to ${gitRemote.get()}")
    }
}
