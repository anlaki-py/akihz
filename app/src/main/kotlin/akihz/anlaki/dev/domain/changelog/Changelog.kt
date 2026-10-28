package akihz.anlaki.dev.domain.changelog

/** One heading, paragraph, or list item from the bundled release notes. */
data class ChangelogBlock(val text: String, val kind: Kind) {
    enum class Kind { Heading, Paragraph, Bullet }
}

/** Release notes in source order, retaining the version and date from their heading. */
data class ChangelogRelease(val version: String, val heading: String, val blocks: List<ChangelogBlock>)

/** The release history bundled with this build. */
data class Changelog(val releases: List<ChangelogRelease>) {
    /**
     * Finds notes for [versionName]. Beta and development builds use their bundled Unreleased snapshot.
     * A missing stable version returns null rather than showing notes for a different version.
     */
    fun forVersion(versionName: String): ChangelogRelease? {
        val version = versionName.removePrefix("v").removeSuffix("-debug")
        releases.firstOrNull { it.version == version }?.let { return it }
        val developmentBuild = versionName.endsWith("-debug") || version == "0.0"
        return if ("-beta." in version || developmentBuild) {
            releases.firstOrNull { it.version == "Unreleased" }
        } else null
    }
}

private val releaseHeading = Regex("^## \\[([^]]+)](.*)$")
private val referenceLink = Regex("^\\[[^]]+]:\\s+.*$")

/**
 * Reads Keep a Changelog headings and their notes from [markdown].
 * Wrapped list items stay together; Markdown link definitions are not displayed as release notes.
 */
fun parseChangelog(markdown: String): Changelog {
    val releases = mutableListOf<ChangelogRelease>()
    var version: String? = null
    var heading = ""
    val body = mutableListOf<String>()

    fun finishRelease() {
        version?.let { releases += ChangelogRelease(it, heading, parseBlocks(body)) }
        body.clear()
    }

    markdown.lineSequence().forEach { line ->
        val match = releaseHeading.matchEntire(line.trim())
        if (match != null) {
            finishRelease()
            version = match.groupValues[1]
            heading = version + match.groupValues[2]
        } else if (version != null && !referenceLink.matches(line.trim())) {
            body += line
        }
    }
    finishRelease()
    return Changelog(releases)
}

private fun parseBlocks(lines: List<String>): List<ChangelogBlock> {
    val blocks = mutableListOf<ChangelogBlock>()
    val pending = StringBuilder()
    var kind = ChangelogBlock.Kind.Paragraph

    fun flush() {
        if (pending.isNotEmpty()) blocks += ChangelogBlock(pending.toString(), kind)
        pending.clear()
    }

    lines.forEach { line ->
        val text = line.trim()
        when {
            text.isEmpty() -> flush()
            text.startsWith("### ") -> {
                flush()
                blocks += ChangelogBlock(text.removePrefix("### "), ChangelogBlock.Kind.Heading)
            }
            text.startsWith("- ") || text.startsWith("* ") -> {
                flush()
                kind = ChangelogBlock.Kind.Bullet
                pending.append(text.drop(2))
            }
            else -> {
                if (pending.isEmpty()) kind = ChangelogBlock.Kind.Paragraph else pending.append(' ')
                pending.append(text)
            }
        }
    }
    flush()
    return blocks
}
