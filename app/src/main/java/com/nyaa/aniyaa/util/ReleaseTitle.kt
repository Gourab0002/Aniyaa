package com.nyaa.aniyaa.util

data class ParsedReleaseTitle(
    val group: String?,
    val show: String?
)

fun parseReleaseTitle(title: String): ParsedReleaseTitle {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return ParsedReleaseTitle(null, null)
    val group = GROUP_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    var rest = if (group != null) trimmed.removePrefix("[$group]").trim() else trimmed
    rest = rest.replace(BRACKET_REGEX, " ").replace(PAREN_REGEX, " ")
    rest = rest.replace(Regex("\\s+"), " ").trim()
    rest = rest.removeSuffix(".mkv").removeSuffix(".mp4").removeSuffix(".avi").trim()
    val episode = EPISODE_REGEX.find(rest)
    val show = if (episode != null) {
        rest.substring(0, episode.range.first).trim().trimEnd('-').trim()
    } else {
        rest
    }.takeIf { it.length >= 2 }
    return ParsedReleaseTitle(
        group = group?.takeIf { it.length in 2..32 && !it.contains("http", ignoreCase = true) },
        show = show?.takeIf { it != group }
    )
}

private val GROUP_REGEX = Regex("^\\[([^\\]]+)]")
private val BRACKET_REGEX = Regex("\\[[^]]+]")
private val PAREN_REGEX = Regex("\\([^)]+\\)")
private val EPISODE_REGEX = Regex(
    "\\s[-–]\\s(?:S\\d+E\\d+|E\\d+|\\d{1,4}(?:\\.\\d+)?)\\b",
    RegexOption.IGNORE_CASE
)
