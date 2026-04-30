package dev.yarallex.shipyard.version

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {

    fun bump(kind: Bump): SemVer = when (kind) {
        Bump.MAJOR -> SemVer(major + 1, 0, 0)
        Bump.MINOR -> SemVer(major, minor + 1, 0)
        Bump.PATCH -> SemVer(major, minor, patch + 1)
        Bump.NONE -> this
    }

    override fun toString(): String = "$major.$minor.$patch"

    override fun compareTo(other: SemVer): Int = compareValuesBy(
        this,
        other,
        SemVer::major,
        SemVer::minor,
        SemVer::patch,
    )

    companion object {
        private val pattern = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

        fun parse(value: String): SemVer {
            val m = pattern.matchEntire(value.trim())
                ?: throw IllegalArgumentException("Not a SemVer string: '$value'")
            val (mj, mn, p) = m.destructured
            return SemVer(mj.toInt(), mn.toInt(), p.toInt())
        }

        fun parseOrNull(value: String): SemVer? = runCatching { parse(value) }.getOrNull()
    }
}

enum class Bump { MAJOR, MINOR, PATCH, NONE }
