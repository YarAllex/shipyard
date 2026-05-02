package dev.yarallex.shipyard

import dev.yarallex.shipyard.version.VersionValueSource
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class ShipyardExtension @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) {

    abstract val imageRepo: Property<String>

    abstract val initialVersion: Property<String>

    abstract val tagPrefix: Property<String>

    abstract val gitRemote: Property<String>

    abstract val registryHost: Property<String>

    abstract val registryUserEnv: Property<String>

    abstract val registryTokenEnv: Property<String>

    abstract val dockerBin: Property<String>

    abstract val gitBin: Property<String>

    abstract val buildTaskName: Property<String>

    abstract val requireCleanWorkingTree: Property<Boolean>

    abstract val envFile: RegularFileProperty

    val currentVersion: Provider<String>
        get() = versionProvider(VersionValueSource.Mode.CURRENT)

    val nextVersion: Provider<String>
        get() = versionProvider(VersionValueSource.Mode.NEXT)

    private fun versionProvider(mode: VersionValueSource.Mode): Provider<String> =
        providers.of(VersionValueSource::class.java) { source ->
            source.parameters.workDir.set(layout.projectDirectory)
            source.parameters.gitBin.set(gitBin)
            source.parameters.tagPrefix.set(tagPrefix)
            source.parameters.initialVersion.set(initialVersion)
            source.parameters.mode.set(mode)
        }
}
