package dev.yarallex.shipyard.env

import java.io.File

object EnvFileLoader {

    fun load(file: File?): Map<String, String> {
        if (file == null || !file.isFile) return emptyMap()
        return file.useLines { lines ->
            lines
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull(::parseLine)
                .toMap()
        }
    }

    private fun parseLine(raw: String): Pair<String, String>? {
        val line = raw.removePrefix("export ").trimStart()
        val eq = line.indexOf('=')
        if (eq <= 0) return null
        val key = line.substring(0, eq).trim()
        if (!isValidKey(key)) return null
        val value = unquote(line.substring(eq + 1).trim())
        return key to value
    }

    private fun isValidKey(key: String): Boolean = key.isNotEmpty() &&
        !key[0].isDigit() &&
        key.all { it.isLetterOrDigit() || it == '_' }

    private fun unquote(value: String): String {
        if (value.length >= 2) {
            val first = value.first()
            val last = value.last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length - 1)
            }
        }
        val hash = value.indexOf(" #")
        return if (hash >= 0) value.substring(0, hash).trimEnd() else value
    }
}
