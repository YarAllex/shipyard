package dev.yarallex.shipyard

import dev.yarallex.shipyard.version.Bump
import dev.yarallex.shipyard.version.ConventionalCommits
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConventionalCommitsTest {

    @Test
    fun `empty log is NONE`() {
        assertEquals(Bump.NONE, ConventionalCommits.analyze(""))
    }

    @Test
    fun `feat header bumps minor`() {
        assertEquals(Bump.MINOR, ConventionalCommits.analyze("feat: add login"))
    }

    @Test
    fun `feat with scope bumps minor`() {
        assertEquals(Bump.MINOR, ConventionalCommits.analyze("feat(auth): google one tap"))
    }

    @Test
    fun `fix bumps patch`() {
        assertEquals(Bump.PATCH, ConventionalCommits.analyze("fix: handle null email"))
    }

    @Test
    fun `bang on header signals breaking`() {
        assertEquals(Bump.MAJOR, ConventionalCommits.analyze("feat!: remove legacy auth"))
    }

    @Test
    fun `BREAKING CHANGE footer signals breaking`() {
        val msg = """
            feat: rework storage layer

            BREAKING CHANGE: minio bucket name now required.
        """.trimIndent()
        assertEquals(Bump.MAJOR, ConventionalCommits.analyze(msg))
    }

    @Test
    fun `BREAKING-CHANGE with hyphen also recognized`() {
        val msg = """
            refactor: rewrite

            BREAKING-CHANGE: api changed
        """.trimIndent()
        assertEquals(Bump.MAJOR, ConventionalCommits.analyze(msg))
    }

    @Test
    fun `breaking takes precedence over feat`() {
        val msg = """
            feat: add x
            ---
            feat!: rip y
        """.trimIndent()
        assertEquals(Bump.MAJOR, ConventionalCommits.analyze(msg))
    }

    @Test
    fun `feat takes precedence over fix`() {
        val msg = """
            fix: tiny patch
            ---
            feat: shiny new thing
        """.trimIndent()
        assertEquals(Bump.MINOR, ConventionalCommits.analyze(msg))
    }

    @Test
    fun `chore commits are NONE`() {
        assertEquals(Bump.NONE, ConventionalCommits.analyze("chore: bump deps"))
    }
}
