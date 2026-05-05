package dev.yarallex.shipyard.exec

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BinaryResolverTest {

    @Test
    fun `absolute existing executable returned as-is`(@TempDir dir: Path) {
        val bin = createExecutable(dir, "tool")
        assertEquals(bin.absolutePath, BinaryResolver.resolve(bin.absolutePath))
    }

    @Test
    fun `absolute non-executable throws`(@TempDir dir: Path) {
        val bin = dir.resolve("missing").toFile()
        assertFailsWith<IllegalArgumentException> {
            BinaryResolver.resolve(bin.absolutePath)
        }
    }

    @Test
    fun `bare name resolved from PATH env`(@TempDir dir: Path) {
        val bin = createExecutable(dir, "myTool")
        val resolved = BinaryResolver.resolve(
            "myTool",
            envPath = dir.toString(),
            fallbackDirs = emptyList(),
        )
        assertEquals(bin.absolutePath, resolved)
    }

    @Test
    fun `bare name resolved from fallback dirs when PATH misses`(@TempDir dir: Path) {
        val bin = createExecutable(dir, "myTool")
        val resolved = BinaryResolver.resolve(
            "myTool",
            envPath = "/nonexistent",
            fallbackDirs = listOf(dir.toString()),
        )
        assertEquals(bin.absolutePath, resolved)
    }

    @Test
    fun `PATH wins over fallback when both contain the binary`(@TempDir tempA: Path, @TempDir tempB: Path) {
        val pathBin = createExecutable(tempA, "tool")
        createExecutable(tempB, "tool")
        val resolved = BinaryResolver.resolve(
            "tool",
            envPath = tempA.toString(),
            fallbackDirs = listOf(tempB.toString()),
        )
        assertEquals(pathBin.absolutePath, resolved)
    }

    @Test
    fun `missing binary throws with helpful message`() {
        val ex = assertFailsWith<IllegalStateException> {
            BinaryResolver.resolve(
                "definitelyNotInstalled_xyz",
                envPath = "/nonexistent",
                fallbackDirs = listOf("/also-nonexistent"),
            )
        }
        val msg = ex.message.orEmpty()
        assertTrue(msg.contains("not found"))
        assertTrue(msg.contains("Searched"))
        assertTrue(msg.contains("definitelyNotInstalled_xyzBin"))
    }

    @Test
    fun `relative path with separator treated as absolute file path`(@TempDir dir: Path) {
        val bin = createExecutable(dir, "tool")
        assertEquals(bin.absolutePath, BinaryResolver.resolve(bin.absolutePath))
    }

    @Test
    fun `augmentedPath merges current PATH with fallback dirs`() {
        val result = BinaryResolver.augmentedPath(
            envPath = "/foo:/bar",
            fallbackDirs = listOf("/baz", "/foo"),
        )
        assertEquals("/foo:/bar:/baz", result)
    }

    @Test
    fun `augmentedPath drops blanks`() {
        val result = BinaryResolver.augmentedPath(
            envPath = "/foo::/bar",
            fallbackDirs = emptyList(),
        )
        assertEquals("/foo:/bar", result)
    }

    private fun createExecutable(dir: Path, name: String): File = dir.resolve(name).toFile().apply {
        writeText("#!/bin/sh\nexit 0\n")
        setExecutable(true)
    }
}
