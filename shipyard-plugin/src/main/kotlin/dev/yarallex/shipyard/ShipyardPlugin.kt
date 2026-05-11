package dev.yarallex.shipyard

import dev.yarallex.shipyard.docker.DockerBuildTask
import dev.yarallex.shipyard.docker.DockerLoginTask
import dev.yarallex.shipyard.git.CreateTagTask
import dev.yarallex.shipyard.git.PushTagTask
import dev.yarallex.shipyard.version.NextVersionTask
import dev.yarallex.shipyard.version.PrintCurrentVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ShipyardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("shipyard", ShipyardExtension::class.java).apply {
            initialVersion.convention("0.1.0")
            tagPrefix.convention("v")
            gitRemote.convention("origin")
            registryHost.convention("ghcr.io")
            registryUserEnv.convention("GHCR_USER")
            registryTokenEnv.convention("GHCR_TOKEN")
            dockerBin.convention("docker")
            gitBin.convention("git")
            platforms.convention(listOf("linux/amd64", "linux/arm64"))
            requireCleanWorkingTree.convention(true)
            envFile.convention(project.layout.projectDirectory.file(".env"))
        }

        val group = "shipyard"

        project.tasks.register(
            "currentVersion",
            PrintCurrentVersionTask::class.java,
        ) {
            it.group = group
            it.description = "Print the current SemVer derived from the latest git tag."
            it.tagPrefix.set(ext.tagPrefix)
            it.initialVersion.set(ext.initialVersion)
            it.gitBin.set(ext.gitBin)
        }

        project.tasks.register(
            "nextVersion",
            NextVersionTask::class.java,
        ) {
            it.group = group
            it.description = "Compute the next SemVer from conventional-commit history."
            it.tagPrefix.set(ext.tagPrefix)
            it.initialVersion.set(ext.initialVersion)
            it.gitBin.set(ext.gitBin)
        }

        val tagVersion = project.tasks.register(
            "tagVersion",
            CreateTagTask::class.java,
        ) {
            it.group = group
            it.description = "Create the next-version git tag locally (no push)."
            it.tagPrefix.set(ext.tagPrefix)
            it.initialVersion.set(ext.initialVersion)
            it.gitBin.set(ext.gitBin)
            it.requireCleanWorkingTree.set(ext.requireCleanWorkingTree)
        }

        val dockerLogin = project.tasks.register(
            "dockerLogin",
            DockerLoginTask::class.java,
        ) {
            it.group = group
            it.description = "Login to the configured Docker registry."
            it.dockerBin.set(ext.dockerBin)
            it.imageRepo.set(ext.imageRepo)
            it.registryHost.set(ext.registryHost)
            it.registryUserEnv.set(ext.registryUserEnv)
            it.registryTokenEnv.set(ext.registryTokenEnv)
            it.envFile.set(ext.envFile)
        }

        val dockerBuild = project.tasks.register(
            "dockerBuild",
            DockerBuildTask::class.java,
        ) {
            it.group = group
            it.description = "Build the Docker image locally (single platform, --load)."
            it.dockerBin.set(ext.dockerBin)
            it.gitBin.set(ext.gitBin)
            it.imageRepo.set(ext.imageRepo)
            it.registryHost.set(ext.registryHost)
            it.tagPrefix.set(ext.tagPrefix)
            it.initialVersion.set(ext.initialVersion)
            it.platforms.set(project.provider { emptyList<String>() })
            it.push.set(false)
            it.mustRunAfter(tagVersion)
        }

        val dockerPush = project.tasks.register(
            "dockerPush",
            DockerBuildTask::class.java,
        ) {
            it.group = group
            it.description = "Build and push image to the registry for all configured platforms."
            it.dockerBin.set(ext.dockerBin)
            it.gitBin.set(ext.gitBin)
            it.imageRepo.set(ext.imageRepo)
            it.registryHost.set(ext.registryHost)
            it.tagPrefix.set(ext.tagPrefix)
            it.initialVersion.set(ext.initialVersion)
            it.platforms.set(ext.platforms)
            it.push.set(true)
            it.dependsOn(dockerLogin)
            it.mustRunAfter(tagVersion)
        }

        project.tasks.register("dockerPushVersion") {
            it.group = group
            it.description = "Deprecated alias for dockerPush."
            it.dependsOn(dockerPush)
        }

        project.tasks.register("dockerPushLatest") {
            it.group = group
            it.description = "Deprecated alias for dockerPush."
            it.dependsOn(dockerPush)
        }

        project.afterEvaluate {
            val name = ext.buildTaskName.orNull
            if (!name.isNullOrBlank()) {
                dockerBuild.configure { task -> task.dependsOn(name) }
                dockerPush.configure { task -> task.dependsOn(name) }
            }
        }

        val pushTag = project.tasks.register(
            "pushTag",
            PushTagTask::class.java,
        ) {
            it.group = group
            it.description = "Push the local version tag to the configured remote."
            it.tagPrefix.set(ext.tagPrefix)
            it.initialVersion.set(ext.initialVersion)
            it.gitBin.set(ext.gitBin)
            it.gitRemote.set(ext.gitRemote)
            it.mustRunAfter(tagVersion, dockerPush)
        }

        project.tasks.register("ship", ReleaseSummaryTask::class.java) {
            it.group = group
            it.description = "Full release: tag the next version, build & push the image, then push the tag."
            it.imageRepo.set(ext.imageRepo)
            it.registryHost.set(ext.registryHost)
            it.tagPrefix.set(ext.tagPrefix)
            it.gitBin.set(ext.gitBin)
            it.gitRemote.set(ext.gitRemote)
            it.dependsOn(tagVersion, dockerPush, pushTag)
            it.mustRunAfter(tagVersion, dockerPush, pushTag)
        }
    }
}
