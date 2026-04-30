package dev.yarallex.shipyard.version

object ConventionalCommits {

    private val breakingHeader = Regex("""(?m)^[a-zA-Z]+(\([^)]*\))?!:""")
    private val breakingFooter = Regex("""(?m)^BREAKING[ -]CHANGE:""")
    private val featHeader = Regex("""(?m)^feat(\([^)]*\))?:""")
    private val fixHeader = Regex("""(?m)^fix(\([^)]*\))?:""")

    fun analyze(log: String): Bump {
        if (log.isBlank()) return Bump.NONE
        return when {
            breakingHeader.containsMatchIn(log) -> Bump.MAJOR
            breakingFooter.containsMatchIn(log) -> Bump.MAJOR
            featHeader.containsMatchIn(log) -> Bump.MINOR
            fixHeader.containsMatchIn(log) -> Bump.PATCH
            else -> Bump.NONE
        }
    }
}
