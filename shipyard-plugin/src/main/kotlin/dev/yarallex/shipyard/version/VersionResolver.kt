package dev.yarallex.shipyard.version

import dev.yarallex.shipyard.git.GitOps
import org.gradle.process.ExecOperations
import java.io.File

object VersionResolver {

    data class Resolved(
        val tagPrefix: String,
        val current: SemVer,
        val next: SemVer,
        val bump: Bump,
        val previousTag: String?,
    ) {
        val currentTag: String get() = previousTag ?: "$tagPrefix$current"
        val nextTag: String get() = "$tagPrefix$next"
    }

    fun resolve(
        execOps: ExecOperations,
        workingDir: File,
        gitBin: String,
        tagPrefix: String,
        initialVersion: String,
    ): Resolved {
        val git = GitOps(execOps, workingDir, gitBin)
        val previousTag = git.latestVersionTag(tagPrefix)
        val current = previousTag
            ?.removePrefix(tagPrefix)
            ?.let { SemVer.parseOrNull(it) }
            ?: SemVer.parse(initialVersion)

        val log = git.logSince(previousTag)
        val bump = if (previousTag == null) Bump.NONE else ConventionalCommits.analyze(log)
        val next = if (previousTag == null) current else current.bump(bump)

        return Resolved(tagPrefix, current, next, bump, previousTag)
    }
}
