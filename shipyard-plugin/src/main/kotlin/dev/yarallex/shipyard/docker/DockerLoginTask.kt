package dev.yarallex.shipyard.docker

import dev.yarallex.shipyard.env.EnvFileLoader
import dev.yarallex.shipyard.log.ShipyardLog
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class DockerLoginTask : DefaultTask() {

    @get:Input
    abstract val dockerBin: Property<String>

    @get:Input
    abstract val registryHost: Property<String>

    @get:Input
    abstract val registryUserEnv: Property<String>

    @get:Input
    abstract val registryTokenEnv: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val envFile: RegularFileProperty

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val styledOutputFactory: StyledTextOutputFactory

    @TaskAction
    fun run() {
        val log = ShipyardLog(styledOutputFactory)
        val userVar = registryUserEnv.get()
        val tokenVar = registryTokenEnv.get()
        val fileVars = EnvFileLoader.load(envFile.orNull?.asFile)
        val user = resolve(userVar, fileVars)
            ?: throw GradleException("'$userVar' is not set in the environment or .env file.")
        val token = resolve(tokenVar, fileVars)
            ?: throw GradleException("'$tokenVar' is not set in the environment or .env file.")

        log.arrow("Logging in to ${registryHost.get()} as $user")
        execOps.exec { spec ->
            spec.commandLine(dockerBin.get(), "login", registryHost.get(), "-u", user, "--password-stdin")
            spec.standardInput = token.byteInputStream()
        }
        log.ok("Logged in: ${registryHost.get()} ($user)")
    }

    private fun resolve(name: String, fileVars: Map<String, String>): String? = System.getenv(name) ?: fileVars[name]
}
