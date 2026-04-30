package dev.yarallex.shipyard.docker

import dev.yarallex.shipyard.log.ShipyardLog
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val styledOutputFactory: StyledTextOutputFactory

    @TaskAction
    fun run() {
        val log = ShipyardLog(styledOutputFactory)
        val userVar = registryUserEnv.get()
        val tokenVar = registryTokenEnv.get()
        val user = System.getenv(userVar)
            ?: throw GradleException("Environment variable '$userVar' is not set.")
        val token = System.getenv(tokenVar)
            ?: throw GradleException("Environment variable '$tokenVar' is not set.")

        log.arrow("Logging in to ${registryHost.get()} as $user")
        execOps.exec { spec ->
            spec.commandLine(dockerBin.get(), "login", registryHost.get(), "-u", user, "--password-stdin")
            spec.standardInput = token.byteInputStream()
        }
        log.ok("Logged in: ${registryHost.get()} ($user)")
    }
}
