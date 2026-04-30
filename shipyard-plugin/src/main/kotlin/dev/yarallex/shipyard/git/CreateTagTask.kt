package dev.yarallex.shipyard.git

import dev.yarallex.shipyard.log.ShipyardLog
import dev.yarallex.shipyard.version.VersionResolver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class CreateTagTask : DefaultTask() {

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val initialVersion: Property<String>

    @get:Input
    abstract val gitBin: Property<String>

    @get:Input
    abstract val requireCleanWorkingTree: Property<Boolean>

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

        if (requireCleanWorkingTree.get() && git.isDirty()) {
            throw GradleException(
                "Working tree is dirty. Commit or stash changes before running 'tagVersion', " +
                    "or set shipyard { requireCleanWorkingTree.set(false) }.",
            )
        }

        val resolved = VersionResolver.resolve(
            execOps,
            workDir,
            gitBin.get(),
            tagPrefix.get(),
            initialVersion.get(),
        )

        if (resolved.previousTag != null && resolved.next == resolved.current) {
            log.warn("No releasable commits since ${resolved.previousTag}; skipping tag.")
            return
        }

        val newTag = resolved.nextTag
        if (git.tagExists(newTag)) {
            log.warn("Tag $newTag already exists; skipping.")
            return
        }

        git.createTag(newTag, "Release $newTag")
        log.ok("Tagged: $newTag (${resolved.bump} bump)")
    }
}
