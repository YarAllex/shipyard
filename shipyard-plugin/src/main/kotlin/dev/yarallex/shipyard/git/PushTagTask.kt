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

abstract class PushTagTask : DefaultTask() {

    @get:Input
    abstract val tagPrefix: Property<String>

    @get:Input
    abstract val initialVersion: Property<String>

    @get:Input
    abstract val gitBin: Property<String>

    @get:Input
    abstract val gitRemote: Property<String>

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
        val remote = gitRemote.get()
        val tag = git.latestVersionTag(tagPrefix.get())
            ?: VersionResolver.resolve(
                execOps,
                workDir,
                gitBin.get(),
                tagPrefix.get(),
                initialVersion.get(),
            ).nextTag

        log.arrow("Pushing tag $tag to $remote")
        val result = git.pushTag(remote, tag)
        if (!result.ok) {
            throw GradleException(buildErrorMessage(remote, tag, git.remoteUrl(remote), result.stderr))
        }
        log.ok("Pushed tag $tag to $remote")
    }

    private fun buildErrorMessage(remote: String, tag: String, remoteUrl: String?, stderr: String): String {
        val hint = remoteUrl?.let(::authHintFor) ?: GENERIC_AUTH_HINT
        return buildString {
            appendLine("git push $remote $tag failed.")
            if (remoteUrl != null) appendLine("Remote URL: $remoteUrl")
            if (stderr.isNotBlank()) {
                appendLine()
                appendLine("git stderr:")
                appendLine(stderr.prependIndent("  "))
            }
            appendLine()
            appendLine(hint)
        }.trimEnd()
    }

    private fun authHintFor(url: String): String = when {
        url.startsWith("git@") || url.startsWith("ssh://") -> SSH_HINT
        url.startsWith("http://") || url.startsWith("https://") -> HTTPS_HINT
        else -> GENERIC_AUTH_HINT
    }

    private companion object {
        const val SSH_HINT = """SSH auth failed. Common fixes:
  - Load your key into ssh-agent: ssh-add ~/.ssh/id_ed25519
  - macOS: ssh-add --apple-use-keychain ~/.ssh/id_ed25519, then add
        AddKeysToAgent yes
        UseKeychain yes
    to ~/.ssh/config under your Host block.
  - Confirm the key is registered with the host: ssh -T git@github.com
  - Or switch the remote to HTTPS: git remote set-url origin https://...
"""

        const val HTTPS_HINT = """HTTPS auth failed. Common fixes:
  - Configure a credential helper:
        macOS:   git config --global credential.helper osxkeychain
        Linux:   git config --global credential.helper store
        Windows: git config --global credential.helper manager
  - Use a Personal Access Token with 'repo' scope as the password.
  - In CI, ensure checkout step persists credentials (actions/checkout sets this by default).
"""

        const val GENERIC_AUTH_HINT = """Verify that the remote is reachable and authenticated.
  See https://git-scm.com/docs/gitcredentials for credential helper options.
"""
    }
}
