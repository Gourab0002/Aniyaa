package com.nyaa.aniyaa.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NyaaCommentParserTest {

    @Test
    fun parse_description_preservesNewlines() {
        val html = """
            <html><body>
            <div id="torrent-description" markdown-text>Line one
Line two

Line three</div>
            </body></html>
        """.trimIndent()
        val result = NyaaCommentParser.parse(html)
        assertTrue("Description should contain newlines for markdown", result.description.contains("\n"))
        assertTrue("First line", result.description.contains("Line one"))
        assertTrue("Second line", result.description.contains("Line two"))
        assertTrue("Third line", result.description.contains("Line three"))
    }

    @Test
    fun parse_description_preservesMarkdownTable() {
        val html = """
            <html><body>
            <div id="torrent-description" markdown-text>| Col1 | Col2 |
| --- | --- |
| A | B |</div>
            </body></html>
        """.trimIndent()
        val result = NyaaCommentParser.parse(html)
        // With .wholeText(), newlines are preserved so the table syntax stays intact
        assertTrue(
            "Table rows should be on separate lines",
            result.description.contains("| Col1 | Col2 |")
        )
        assertTrue(result.description.contains("| --- | --- |"))
        assertTrue(result.description.contains("| A | B |"))
    }

    @Test
    fun parse_comment_content_preservesNewlines() {
        val html = """
            <html><body>
            <div id="comments">
              <div class="comment-panel" id="com-1">
                <div class="panel-body">
                  <div class="col-md-2">
                    <a href="/user/testuser">testuser</a>
                  </div>
                  <div class="col-md-10 comment">
                    <div class="row comment-details">
                      <a href="#com-1"><small>2024-01-01</small></a>
                    </div>
                    <div class="row comment-body">
                      <div class="comment-content" markdown-text>Paragraph one

Paragraph two</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            </body></html>
        """.trimIndent()
        val result = NyaaCommentParser.parse(html)
        assertEquals(1, result.comments.size)
        val content = result.comments[0].content
        assertTrue("Comment content should preserve newlines", content.contains("\n"))
        assertTrue(content.contains("Paragraph one"))
        assertTrue(content.contains("Paragraph two"))
    }

    @Test
    fun parse_comment_extractsAvatarUrl() {
        val html = """
            <html><body>
            <div id="comments">
              <div class="comment-panel" id="com-1">
                <div class="panel-body">
                  <div class="col-md-2">
                    <a href="/user/testuser">testuser</a>
                    <img class="avatar" src="//nyaa.si/avatars/testuser.png" />
                  </div>
                  <div class="col-md-10 comment">
                    <div class="row comment-details">
                      <a href="#com-1"><small>2024-01-01</small></a>
                    </div>
                    <div class="row comment-body">
                      <div class="comment-content" markdown-text>Some comment</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            </body></html>
        """.trimIndent()
        val result = NyaaCommentParser.parse(html)
        assertEquals(1, result.comments.size)
        assertEquals("https://nyaa.si/avatars/testuser.png", result.comments[0].avatarUrl)
    }

    @Test
    fun parse_emptyDescription_returnsEmpty() {
        val html = "<html><body></body></html>"
        val result = NyaaCommentParser.parse(html)
        assertEquals("", result.description)
        assertTrue(result.comments.isEmpty())
    }
}
