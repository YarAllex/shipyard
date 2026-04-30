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

abstract class NextVersionTask : DefaultTask() {

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
        log.ok("Current: ${resolved.current} (${resolved.currentTag})")
        if (resolved.previousTag == null) {
            log.arrow("Next:    ${resolved.next} (${resolved.nextTag}, initial release)")
        } else if (resolved.bump == Bump.NONE) {
            log.arrow("Next:    ${resolved.next} (no releasable commits since ${resolved.previousTag})")
        } else {
            log.arrow("Next:    ${resolved.next} (${resolved.nextTag}, ${resolved.bump} bump)")
        }
    }
}
