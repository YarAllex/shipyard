package dev.yarallex.shipyard.version

import dev.yarallex.shipyard.log.ShipyardLog
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class PrintCurrentVersionTask : DefaultTask() {

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val initialVersion: Property<String>

    @get:Input
    abstract val gitBin: Property<String>

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val styledOutputFactory: StyledTextOutputFactory

    @TaskAction
    fun run() {
        val log = ShipyardLog(styledOutputFactory)
        val resolved = VersionResolver.resolve(
            execOps,
            layout.projectDirectory.asFile,
            gitBin.get(),
            tagPrefix.get(),
            initialVersion.get(),
        )
        if (resolved.previousTag == null) {
            log.arrow("No release tags yet; using initial version.")
        }
        log.ok("Current: ${resolved.current} (${resolved.currentTag})")
    }
}
