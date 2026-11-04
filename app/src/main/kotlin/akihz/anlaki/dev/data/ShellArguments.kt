package akihz.anlaki.dev.data

/** Quotes [value] as one POSIX shell argument, including embedded apostrophes. */
internal fun quoteShellArgument(value: String): String =
    "'" + value.replace("'", "'\"'\"'") + "'"
