package dev.yarallex.shipyard.exec

import java.io.File

object BinaryResolver {

    private val COMMON_PATHS = listOf(
        "/opt/homebrew/bin",
        "/usr/local/bin",
        "/usr/bin",
        "/bin",
        "/usr/sbin",
        "/sbin",
    )

    fun augmentedPath(envPath: String? = System.getenv("PATH"), fallbackDirs: List<String> = COMMON_PATHS): String {
        val current = envPath?.split(File.pathSeparator).orEmpty()
        val merged = (current + fallbackDirs).filter { it.isNotBlank() }.distinct()
        return merged.joinToString(File.pathSeparator)
    }

    fun resolve(
        name: String,
        envPath: String? = System.getenv("PATH"),
        fallbackDirs: List<String> = COMMON_PATHS,
    ): String {
        if (name.contains(File.separatorChar)) {
            val file = File(name)
            require(file.isFile && file.canExecute()) {
                "Binary at '$name' is not executable or does not exist."
            }
            return file.absolutePath
        }

        val pathDirs = envPath?.split(File.pathSeparator).orEmpty()
        val candidateDirs = (pathDirs + fallbackDirs).filter { it.isNotBlank() }.distinct()

        for (dir in candidateDirs) {
            val candidate = File(dir, name)
            if (candidate.isFile && candidate.canExecute()) {
                return candidate.absolutePath
            }
        }

        throw IllegalStateException(notFoundMessage(name, candidateDirs))
    }

    private fun notFoundMessage(name: String, searched: List<String>): String = buildString {
        appendLine("'$name' executable not found.")
        appendLine()
        appendLine("Searched:")
        searched.take(12).forEach { appendLine("  - $it") }
        if (searched.size > 12) appendLine("  ... and ${searched.size - 12} more")
        appendLine()
        appendLine("Fixes:")
        appendLine("  - Install $name and ensure it is on PATH.")
        appendLine("  - Set an absolute path explicitly in shipyard { ${name}Bin = \"/full/path/to/$name\" }")
        appendLine("  - GUI apps on macOS often miss /usr/local/bin and /opt/homebrew/bin in PATH;")
        appendLine("    run 'sudo launchctl config user path' or set PATH in the IDE run configuration.")
    }
}
