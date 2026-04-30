package dev.yarallex.shipyard.docker

object ImageRef {

    fun qualify(repo: String, registryHost: String): String {
        val first = repo.substringBefore('/', missingDelimiterValue = repo)
        return if (looksLikeHost(first) || registryHost.isBlank()) repo else "$registryHost/$repo"
    }

    fun extractHost(repo: String): String? {
        val first = repo.substringBefore('/', missingDelimiterValue = "")
        return if (first.isNotEmpty() && looksLikeHost(first)) first else null
    }

    fun effectiveHost(repo: String, fallback: String): String = extractHost(repo) ?: fallback

    private fun looksLikeHost(segment: String): Boolean =
        segment == "localhost" || segment.contains('.') || segment.contains(':')
}
