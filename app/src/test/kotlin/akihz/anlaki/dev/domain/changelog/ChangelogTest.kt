package akihz.anlaki.dev.domain.changelog

import org.junit.Assert.*
import org.junit.Test

class ChangelogTest {
    private val markdown = """
        # Changelog

        ## [Unreleased]
        ### Added
        - Read notes offline.

        ## [0.0.50] - 2026-10-26
        ### Changed
        - A wrapped change
          continues here.
        - Another change.

        A release note outside the list.

        [Unreleased]: https://example.com/compare
        [0.0.50]: https://example.com/release
    """.trimIndent()

    @Test
    fun `stable installs select their own notes`() {
        val changelog = parseChangelog(markdown)
        val release = changelog.forVersion("0.0.50")!!
        assertEquals("0.0.50 - 2026-10-26", release.heading)
        assertEquals(listOf("Changed", "A wrapped change continues here.", "Another change.",
            "A release note outside the list."), release.blocks.map { it.text })
        assertEquals(ChangelogBlock.Kind.Bullet, release.blocks[1].kind)
    }

    @Test
    fun `beta installs use the bundled unreleased snapshot`() {
        val changelog = parseChangelog(markdown)
        assertEquals("Unreleased", changelog.forVersion("0.0.51-beta.4")?.version)
        assertEquals("Unreleased", changelog.forVersion("0.0-debug")?.version)
        assertEquals("0.0.50", changelog.forVersion("0.0.50-debug")?.version)
    }

    @Test
    fun `explicit beta notes take priority over unreleased`() {
        val changelog = parseChangelog(markdown + "\n## [0.0.51-beta.4]\n- Notes for this beta.")
        assertEquals("0.0.51-beta.4", changelog.forVersion("0.0.51-beta.4")?.version)
    }

    @Test
    fun `missing stable notes do not show a different version`() {
        val changelog = parseChangelog(markdown)
        assertNull(changelog.forVersion("0.0.49"))
        assertEquals(2, changelog.releases.size)
        assertTrue(parseChangelog("").releases.isEmpty())
    }
}
