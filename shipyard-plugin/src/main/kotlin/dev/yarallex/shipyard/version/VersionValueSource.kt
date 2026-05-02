package dev.yarallex.shipyard.version

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class VersionValueSource : ValueSource<String, VersionValueSource.Params> {

    enum class Mode { CURRENT, NEXT }

    interface Params : ValueSourceParameters {
        val workDir: DirectoryProperty
        val gitBin: Property<String>
        val tagPrefix: Property<String>
        val initialVersion: Property<String>
        val mode: Property<Mode>
    }

    @get:Inject
    abstract val execOps: ExecOperations

    override fun obtain(): String {
        val resolved = VersionResolver.resolve(
            execOps,
            parameters.workDir.get().asFile,
            parameters.gitBin.get(),
            parameters.tagPrefix.get(),
            parameters.initialVersion.get(),
        )
        return when (parameters.mode.get()) {
            Mode.CURRENT -> resolved.current.toString()
            Mode.NEXT -> resolved.next.toString()
        }
    }
}
