package dev.yarallex.shipyard.env

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvFileLoaderTest {

    @Test
    fun `null file yields empty map`() {
        assertTrue(EnvFileLoader.load(null).isEmpty())
    }

    @Test
    fun `missing file yields empty map`(@TempDir dir: Path) {
        val file = dir.resolve("missing.env").toFile()
        assertTrue(EnvFileLoader.load(file).isEmpty())
    }

    @Test
    fun `parses bare key=value pairs`(@TempDir dir: Path) {
        val file = write(
            dir,
            """
            FOO=bar
            BAZ=qux
            """.trimIndent(),
        )
        assertEquals(mapOf("FOO" to "bar", "BAZ" to "qux"), EnvFileLoader.load(file))
    }

    @Test
    fun `skips blanks and comments`(@TempDir dir: Path) {
        val file = write(
            dir,
            """
            # leading comment
            FOO=bar

            # another
            BAZ=qux
            """.trimIndent(),
        )
        assertEquals(mapOf("FOO" to "bar", "BAZ" to "qux"), EnvFileLoader.load(file))
    }

    @Test
    fun `strips export prefix`(@TempDir dir: Path) {
        val file = write(dir, "export FOO=bar")
        assertEquals(mapOf("FOO" to "bar"), EnvFileLoader.load(file))
    }

    @Test
    fun `unwraps double and single quotes`(@TempDir dir: Path) {
        val file = write(
            dir,
            """
            FOO="hello world"
            BAR='single quoted'
            """.trimIndent(),
        )
        assertEquals(mapOf("FOO" to "hello world", "BAR" to "single quoted"), EnvFileLoader.load(file))
    }

    @Test
    fun `strips trailing inline comment for unquoted values`(@TempDir dir: Path) {
        val file = write(dir, "FOO=bar # inline comment")
        assertEquals(mapOf("FOO" to "bar"), EnvFileLoader.load(file))
    }

    @Test
    fun `keeps hash inside quoted values`(@TempDir dir: Path) {
        val file = write(dir, "FOO=\"bar # not a comment\"")
        assertEquals(mapOf("FOO" to "bar # not a comment"), EnvFileLoader.load(file))
    }

    @Test
    fun `rejects keys starting with digit`(@TempDir dir: Path) {
        val file = write(dir, "1FOO=bar")
        assertTrue(EnvFileLoader.load(file).isEmpty())
    }

    @Test
    fun `rejects keys with invalid characters`(@TempDir dir: Path) {
        val file = write(dir, "FOO-BAR=baz")
        assertTrue(EnvFileLoader.load(file).isEmpty())
    }

    @Test
    fun `last occurrence wins on duplicate keys`(@TempDir dir: Path) {
        val file = write(
            dir,
            """
            FOO=first
            FOO=second
            """.trimIndent(),
        )
        assertEquals(mapOf("FOO" to "second"), EnvFileLoader.load(file))
    }

    private fun write(dir: Path, content: String): File = dir.resolve(".env").toFile().also { it.writeText(content) }
}
