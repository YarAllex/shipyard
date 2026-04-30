package dev.yarallex.shipyard.git

data class GitPushResult(val exitCode: Int, val stderr: String) {
    val ok: Boolean get() = exitCode == 0
}
