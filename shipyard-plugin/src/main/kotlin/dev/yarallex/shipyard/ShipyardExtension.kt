package dev.yarallex.shipyard

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class ShipyardExtension {

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
}
