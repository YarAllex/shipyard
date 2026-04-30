package dev.yarallex.shipyard.docker

object ImageRef {

    fun qualify(repo: String, registryHost: String): String {
        val first = repo.substringBefore('/', missingDelimiterValue = repo)
        return if (looksLikeHost(first) || registryHost.isBlank()) repo else "$registryHost/$repo"
    }

    private fun looksLikeHost(segment: String): Boolean =
        segment == "localhost" || segment.contains('.') || segment.contains(':')
}
