package com.nyaa.aniyaa.util

sealed class DescriptionBlock {
    data class Markdown(val text: String) : DescriptionBlock()
    data class Gallery(val images: List<DescriptionImage>) : DescriptionBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : DescriptionBlock()
    data class Code(val body: String) : DescriptionBlock()
}

data class DescriptionImage(
    val url: String,
    val alt: String = ""
)

object DescriptionFormatter {

    fun prepare(raw: String): String {
        return normalizeWhitespace(convertBbcode(raw.trim()))
    }

    fun blocks(raw: String): List<DescriptionBlock> {
        val prepared = prepare(raw)
        if (prepared.isBlank()) return emptyList()
        return splitBlocks(prepared)
    }

    internal fun convertBbcode(input: String): String {
        var text = input.replace("\r\n", "\n").replace('\r', '\n')
        text = BB_IMG.replace(text) { match ->
            val url = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }.orEmpty().trim()
            if (url.isEmpty()) match.value else "![]($url)"
        }
        text = BB_URL_LABELED.replace(text) { "[${it.groupValues[2]}](${it.groupValues[1].trim()})" }
        text = BB_URL.replace(text) { "[${it.groupValues[1]}](${it.groupValues[1].trim()})" }
        text = BB_BOLD.replace(text, "**$1**")
        text = BB_ITALIC.replace(text, "*$1*")
        text = BB_STRIKE.replace(text, "~~$1~~")
        text = BB_UNDER.replace(text, "$1")
        text = BB_CODE_BLOCK.replace(text) { "\n```\n${it.groupValues[1].trim()}\n```\n" }
        text = BB_QUOTE.replace(text) { quote ->
            quote.groupValues[1].trim().lines().joinToString("\n") { "> $it" } + "\n"
        }
        text = BB_LIST_ITEM.replace(text, "\n- ")
        text = BB_LIST.replace(text, "$1")
        text = BB_SPOILER.replace(text) { "\n**Spoiler**\n\n${it.groupValues[1].trim()}\n" }
        text = BB_ALIGN.replace(text, "$1")
        text = BB_STYLE.replace(text, "$1")
        text = BB_HR.replace(text, "\n\n---\n\n")
        text = BB_LEFTOVER.replace(text, "")
        return text
    }

    internal fun normalizeWhitespace(input: String): String {
        return input
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun splitBlocks(input: String): List<DescriptionBlock> {
        val lines = input.split('\n')
        val blocks = ArrayList<DescriptionBlock>()
        val buffer = StringBuilder()

        fun flushMarkdown() {
            val text = buffer.toString().trim()
            buffer.setLength(0)
            if (text.isNotEmpty()) blocks += DescriptionBlock.Markdown(text)
        }

        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            when {
                line.trim().startsWith("```") -> {
                    flushMarkdown()
                    val body = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        if (body.isNotEmpty()) body.append('\n')
                        body.append(lines[index])
                        index++
                    }
                    blocks += DescriptionBlock.Code(body.toString())
                    if (index < lines.size) index++
                }
                isTableSeparatorContext(lines, index) -> {
                    flushMarkdown()
                    val tableLines = ArrayList<String>()
                    while (index < lines.size && lines[index].contains('|')) {
                        tableLines += lines[index]
                        index++
                    }
                    parseTable(tableLines)?.let { blocks += it }
                }
                imageFromLine(line) != null -> {
                    flushMarkdown()
                    val images = ArrayList<DescriptionImage>()
                    while (index < lines.size) {
                        val image = imageFromLine(lines[index])
                        if (image == null) {
                            if (lines[index].isBlank() &&
                                index + 1 < lines.size &&
                                imageFromLine(lines[index + 1]) != null
                            ) {
                                index++
                                continue
                            }
                            break
                        }
                        images += image
                        index++
                    }
                    if (images.size == 1) {
                        buffer.append("![").append(images[0].alt).append("](").append(images[0].url).append(')')
                    } else {
                        blocks += DescriptionBlock.Gallery(images)
                    }
                }
                else -> {
                    if (buffer.isNotEmpty()) buffer.append('\n')
                    buffer.append(line)
                    index++
                }
            }
        }
        flushMarkdown()
        return blocks.ifEmpty { listOf(DescriptionBlock.Markdown(input)) }
    }

    private fun isTableSeparatorContext(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size) return false
        if (!lines[index].contains('|')) return false
        return isTableSeparator(lines[index + 1])
    }

    private fun isTableSeparator(line: String): Boolean {
        val cells = splitTableRow(line)
        return cells.isNotEmpty() && cells.all { cell ->
            val trimmed = cell.trim()
            trimmed.isNotEmpty() && trimmed.all { it == '-' || it == ':' } && trimmed.contains('-')
        }
    }

    private fun parseTable(tableLines: List<String>): DescriptionBlock.Table? {
        val rows = tableLines
            .filter { !isTableSeparator(it) }
            .map { splitTableRow(it) }
            .filter { it.isNotEmpty() }
        if (rows.isEmpty()) return null
        val headers = rows.first()
        val body = rows.drop(1).map { row ->
            if (row.size >= headers.size) {
                row.take(headers.size)
            } else {
                row + List(headers.size - row.size) { "" }
            }
        }
        return DescriptionBlock.Table(headers, body)
    }

    private fun splitTableRow(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split('|').map { it.trim() }
    }

    internal fun imageFromLine(line: String): DescriptionImage? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val markdown = MARKDOWN_IMAGE.matchEntire(trimmed)
        if (markdown != null) {
            val url = markdown.groupValues[2].trim()
            if (isSafeHttpUrl(url)) return DescriptionImage(url, markdown.groupValues[1].trim())
        }
        val bare = BARE_IMAGE.matchEntire(trimmed)
        if (bare != null && isSafeHttpUrl(trimmed)) {
            return DescriptionImage(trimmed)
        }
        return null
    }

    private val BB_IMG = Regex(
        """\[img(?:\s*=\s*"?([^\]"\s]+)"?)?]\s*(.*?)\s*\[/img]|\[img=([^\]]+)]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_URL_LABELED = Regex(
        """\[url=([^\]]+)](.*?)\[/url]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_URL = Regex(
        """\[url](.*?)\[/url]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_BOLD = Regex("""\[b](.*?)\[/b]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_ITALIC = Regex("""\[i](.*?)\[/i]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_STRIKE = Regex("""\[s](.*?)\[/s]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_UNDER = Regex("""\[u](.*?)\[/u]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_CODE_BLOCK = Regex(
        """\[code](.*?)\[/code]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_QUOTE = Regex(
        """\[quote(?:=[^\]]+)?](.*?)\[/quote]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_LIST = Regex(
        """\[list](.*?)\[/list]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_LIST_ITEM = Regex("""\[\*]|\[li]""", RegexOption.IGNORE_CASE)
    private val BB_SPOILER = Regex(
        """\[spoiler(?:=[^\]]+)?](.*?)\[/spoiler]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_ALIGN = Regex(
        """\[(?:center|left|right)](.*?)\[/(?:center|left|right)]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_STYLE = Regex(
        """\[(?:color|size|font)=[^\]]+](.*?)\[/(?:color|size|font)]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_HR = Regex("""\[hr/?]""", RegexOption.IGNORE_CASE)
    private val BB_LEFTOVER = Regex(
        """\[/?(?:b|i|u|s|img|url|quote|code|list|center|left|right|color|size|font|spoiler|hr|li)(?:=[^\]]*)?]""",
        RegexOption.IGNORE_CASE
    )
    private val MARKDOWN_IMAGE = Regex("""^!\[(.*?)]\((https?://[^)\s]+)\)$""")
    private val BARE_IMAGE = Regex("""^https?://\S+\.(?:png|jpe?g|gif|webp|avif)(?:\?\S*)?$""", RegexOption.IGNORE_CASE)
}
