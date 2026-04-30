package dev.yarallex.shipyard

import dev.yarallex.shipyard.version.Bump
import dev.yarallex.shipyard.version.SemVer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SemVerTest {

    @Test
    fun `parses dotted triple`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
    }

    @Test
    fun `parseOrNull returns null on bad input`() {
        assertNull(SemVer.parseOrNull("v1.2"))
        assertNull(SemVer.parseOrNull("1.2"))
        assertNull(SemVer.parseOrNull("nope"))
    }

    @Test
    fun `parse rejects malformed`() {
        assertFailsWith<IllegalArgumentException> { SemVer.parse("1.2") }
    }

    @Test
    fun `bump major resets minor and patch`() {
        assertEquals(SemVer(2, 0, 0), SemVer(1, 5, 7).bump(Bump.MAJOR))
    }

    @Test
    fun `bump minor resets patch`() {
        assertEquals(SemVer(1, 6, 0), SemVer(1, 5, 7).bump(Bump.MINOR))
    }

    @Test
    fun `bump patch increments only patch`() {
        assertEquals(SemVer(1, 5, 8), SemVer(1, 5, 7).bump(Bump.PATCH))
    }

    @Test
    fun `bump none returns same`() {
        val v = SemVer(1, 5, 7)
        assertEquals(v, v.bump(Bump.NONE))
    }

    @Test
    fun `toString matches dotted triple`() {
        assertEquals("3.0.1", SemVer(3, 0, 1).toString())
    }

    @Test
    fun `compares lexicographically`() {
        assert(SemVer(1, 0, 0) < SemVer(1, 0, 1))
        assert(SemVer(1, 0, 5) < SemVer(1, 1, 0))
        assert(SemVer(1, 9, 9) < SemVer(2, 0, 0))
    }
}
