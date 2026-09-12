package com.nyaa.aniyaa.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NyaaCommentParserTest {

    @Test
    fun parse_readsDescriptionCommentsAndNestedFiles() {
        val html = """
            <html>
              <body>
                <div class="panel panel-success">
                  <div class="panel-heading"><h3 class="panel-title">Release Title</h3></div>
                  <div class="row"><div class="col-md-5">Submitter:</div><div class="col-md-7"><a href="/user/bob">bob</a></div></div>
                </div>
                <div id="torrent-description">Line 1
Line 2</div>
                <div class="torrent-file-list">
                  <ul>
                    <li class="torrent-file-list-folder">
                      folderName
                      <ul>
                        <li>inner.mp4 <span class="pull-right">800.0 MiB</span></li>
                      </ul>
                    </li>
                    <li>root.txt <span class="pull-right">200.0 MiB</span></li>
                  </ul>
                </div>
                <div id="comments">
                  <div class="comment-panel" id="com-9">
                    <a href="/user/alice">alice</a>
                    <img class="avatar" src="/static/img/avatar.png" />
                    <div class="comment-body">
                      <div class="comment-content">Nice release</div>
                    </div>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

        val page = NyaaCommentParser.parse(html, "https://nyaa.si")
        assertEquals("Line 1\nLine 2", page.description)
        assertEquals(2, page.fileList.size)
        assertEquals("folderName/inner.mp4", page.fileList[0].name)
        assertEquals("800.0 MiB", page.fileList[0].size)
        assertEquals("root.txt", page.fileList[1].name)
        assertEquals(1, page.comments.size)
        assertEquals("alice", page.comments[0].username)
        assertEquals("https://nyaa.si/static/img/avatar.png", page.comments[0].avatarUrl)
        assertEquals("Nice release", page.comments[0].content)
        assertEquals("9", page.comments[0].id)
        assertEquals("bob", page.submitter)
        assertEquals("Release Title", page.title)
        assertEquals("200.0 MiB", page.fileList[1].size)
    }

    @Test
    fun parse_readsNyaaFileSizeSpansAndFolderLinks() {
        val html = """
            <html><body>
              <div class="torrent-file-list panel-body">
                <ul>
                  <li>
                    <a href="" class="folder"><i class="fa fa-folder"></i>Show</a>
                    <ul>
                      <li><i class="fa fa-file"></i>Episode.01.mkv <span class="file-size">(1.4 GiB)</span></li>
                      <li><i class="fa fa-file"></i>Episode.02.mkv <span class="file-size">(1.3 GiB)</span></li>
                    </ul>
                  </li>
                  <li><i class="fa fa-file"></i>readme.txt <span class="file-size">(12.0 KiB)</span></li>
                </ul>
              </div>
            </body></html>
        """.trimIndent()
        val page = NyaaCommentParser.parse(html, "https://nyaa.si")
        assertEquals(3, page.fileList.size)
        assertEquals("Show/Episode.01.mkv", page.fileList[0].name)
        assertEquals("1.4 GiB", page.fileList[0].size)
        assertEquals("Show/Episode.02.mkv", page.fileList[1].name)
        assertEquals("1.3 GiB", page.fileList[1].size)
        assertEquals("readme.txt", page.fileList[2].name)
        assertEquals("12.0 KiB", page.fileList[2].size)
        assertTrue(page.fileList.none { it.name.contains("GiB") || it.name.contains("KiB") })
    }

    @Test
    fun parse_convertsRenderedHtmlDescriptionToMarkdown() {
        val html = """
            <html><body>
              <div id="torrent-description">
                <h2>Screens</h2>
                <p>Compare <strong>HEVC</strong> vs AVC.</p>
                <img src="/img/a.png" alt="one" />
                <table><tr><th>Codec</th><th>Size</th></tr><tr><td>HEVC</td><td>1 GiB</td></tr></table>
              </div>
            </body></html>
        """.trimIndent()
        val page = NyaaCommentParser.parse(html, "https://nyaa.si")
        assertTrue(page.description.contains("## Screens"))
        assertTrue(page.description.contains("**HEVC**"))
        assertTrue(page.description.contains("![one](https://nyaa.si/img/a.png)"))
        assertTrue(page.description.contains("| Codec | Size |"))
        assertTrue(page.description.contains("| HEVC | 1 GiB |"))
    }
}
