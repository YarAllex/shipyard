package dev.yarallex.shipyard.docker

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ImageRefTest {

    @Test
    fun `prepends host when repo has no host segment`() {
        assertEquals("ghcr.io/acme/api", ImageRef.qualify("acme/api", "ghcr.io"))
    }

    @Test
    fun `keeps repo unchanged when first segment matches host`() {
        assertEquals("ghcr.io/acme/api", ImageRef.qualify("ghcr.io/acme/api", "ghcr.io"))
    }

    @Test
    fun `respects explicit different host`() {
        assertEquals("docker.io/acme/api", ImageRef.qualify("docker.io/acme/api", "ghcr.io"))
    }

    @Test
    fun `recognises localhost as host`() {
        assertEquals("localhost/acme/api", ImageRef.qualify("localhost/acme/api", "ghcr.io"))
    }

    @Test
    fun `recognises host with port as host`() {
        assertEquals("localhost:5000/acme/api", ImageRef.qualify("localhost:5000/acme/api", "ghcr.io"))
    }

    @Test
    fun `recognises private registry as host`() {
        assertEquals(
            "registry.gitlab.com/acme/api",
            ImageRef.qualify("registry.gitlab.com/acme/api", "ghcr.io"),
        )
    }

    @Test
    fun `prepends host for single-segment repo`() {
        assertEquals("ghcr.io/api", ImageRef.qualify("api", "ghcr.io"))
    }

    @Test
    fun `returns repo as-is when host is blank`() {
        assertEquals("acme/api", ImageRef.qualify("acme/api", ""))
    }
}
