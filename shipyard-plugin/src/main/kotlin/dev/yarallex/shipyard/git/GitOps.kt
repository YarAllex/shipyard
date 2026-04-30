package dev.yarallex.shipyard.git

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

class GitOps(private val execOps: ExecOperations, private val workingDir: File, private val gitBin: String = "git") {

    fun latestVersionTag(prefix: String): String? {
        val out = capture(listOf(gitBin, "describe", "--tags", "--abbrev=0", "--match=$prefix*"))
            ?: return null
        return out.takeIf { it.isNotBlank() }
    }

    fun logSince(rangeFromInclusiveExclusive: String?): String {
        val args = mutableListOf(gitBin, "log", "--format=%B%n---")
        if (rangeFromInclusiveExclusive != null) {
            args += "$rangeFromInclusiveExclusive..HEAD"
        }
        return capture(args).orEmpty()
    }

    fun tagExists(tag: String): Boolean {
        val out = capture(listOf(gitBin, "tag", "--list", tag)).orEmpty()
        return out.lines().any { it.trim() == tag }
    }

    fun createTag(tag: String, message: String) {
        run(listOf(gitBin, "tag", "-a", tag, "-m", message))
    }

    fun pushTag(remote: String, tag: String) {
        run(listOf(gitBin, "push", remote, tag))
    }

    fun isDirty(): Boolean {
        val out = capture(listOf(gitBin, "status", "--porcelain")).orEmpty()
        return out.isNotBlank()
    }

    private fun run(cmd: List<String>) {
        execOps.exec { spec ->
            spec.workingDir(this.workingDir)
            spec.commandLine(cmd)
        }
    }

    private fun capture(cmd: List<String>): String? {
        val stdout = ByteArrayOutputStream()
        val result = execOps.exec { spec ->
            spec.workingDir(this.workingDir)
            spec.commandLine(cmd)
            spec.standardOutput = stdout
            spec.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) return null
        return stdout.toString(Charsets.UTF_8).trim()
    }
}
