package com.pamaz.vi.core.data.news

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val RSS_VALID = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
  <channel>
    <title>Example News</title>
    <link>https://example.com/</link>
    <description>Fixture feed for fetch-news tests.</description>
    <item>
      <title>First story with an enclosure image</title>
      <link>https://example.com/articles/first-story</link>
      <guid isPermaLink="false">urn:example:first-story</guid>
      <pubDate>Mon, 03 Nov 2026 09:00:00 GMT</pubDate>
      <description><![CDATA[<p>A short <b>plain</b> summary of the first story.</p>]]></description>
      <enclosure url="https://example.com/images/first-story.jpg" type="image/jpeg" length="12345" />
    </item>
    <item>
      <title>Second story with a media:content image</title>
      <link>https://example.com/articles/second-story</link>
      <guid>https://example.com/articles/second-story</guid>
      <pubDate>Sun, 02 Nov 2026 18:30:00 GMT</pubDate>
      <description>Plain-text summary, no HTML at all.</description>
      <media:content url="https://example.com/images/second-story.png" medium="image" />
    </item>
    <item>
      <title>Third story with no image</title>
      <link>https://example.com/articles/third-story</link>
      <pubDate>Sat, 01 Nov 2026 07:15:00 GMT</pubDate>
      <description>No enclosure or media tags here.</description>
    </item>
  </channel>
</rss>
"""

private const val ATOM_VALID = """<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
  <title>Example Atom Feed</title>
  <id>urn:example:atom-feed</id>
  <updated>2026-11-03T09:00:00Z</updated>
  <entry>
    <title>Atom story with alternate link and thumbnail</title>
    <id>urn:example:atom-story-1</id>
    <link rel="self" href="https://example.com/feed/atom-story-1.atom" />
    <link rel="alternate" href="https://example.com/articles/atom-story-1" />
    <updated>2026-11-03T08:45:00Z</updated>
    <published>2026-11-03T08:30:00Z</published>
    <summary>A short summary of the first Atom story.</summary>
    <media:thumbnail url="https://example.com/images/atom-story-1.jpg" />
  </entry>
  <entry>
    <title>Atom story with only one link and HTML content</title>
    <id>urn:example:atom-story-2</id>
    <link href="https://example.com/articles/atom-story-2" />
    <updated>2026-11-02T12:00:00Z</updated>
    <content type="html">&lt;p&gt;HTML-escaped &lt;strong&gt;content&lt;/strong&gt; body.&lt;/p&gt;</content>
  </entry>
</feed>
"""

private const val RSS_MALFORMED = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Broken Feed</title>
    <item>
      <title>Unclosed tag below breaks the whole document
      <link>https://example.com/articles/broken</link>
    </item>
  </channel>
</rss>
"""

/** Some feeds embed raw (non-CDATA), truly un-escaped HTML elements straight inside `<description>`. */
private const val RSS_RAW_HTML_DESCRIPTION = """<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
  <channel>
    <title>Raw-HTML Feed</title>
    <item>
      <title>Grand Theft Auto VI Pre-Load Begins on November 10 - Rockstar Games</title>
      <link>https://news.google.com/rss/articles/first-story?oc=5</link>
      <guid isPermaLink="false">CBMisgFfirst</guid>
      <pubDate>Tue, 03 Nov 2026 15:00:03 GMT</pubDate>
      <description><a href="https://news.google.com/rss/articles/first-story?oc=5" target="_blank">Grand Theft Auto VI Pre-Load Begins on November 10</a>&nbsp;&nbsp;<font color="#6f6f6f">Rockstar Games</font></description>
      <source url="https://www.rockstargames.com">Rockstar Games</source>
    </item>
  </channel>
</rss>
"""

/**
 * The real Google News RSS feed's actual convention (confirmed by fetching
 * `news.google.com/rss/search` live, not assumed from a sample): `<description>` is fully
 * HTML-entity-escaped plain text (`&lt;a ...&gt;...&lt;/a&gt;`, not real child elements), and its
 * embedded `&nbsp;` is itself re-escaped to `&amp;nbsp;` — a double-escape that needs two
 * [decodeEntities] passes (one implicit in parsing, one inside [stripHtml]) to fully resolve.
 */
private const val RSS_DOUBLE_ESCAPED_DESCRIPTION = """<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
  <channel>
    <title>Google News</title>
    <item>
      <title>Grand Theft Auto VI Pre-Load Begins on November 10 - Rockstar Games</title>
      <link>https://news.google.com/rss/articles/first-story?oc=5</link>
      <guid isPermaLink="false">CBMisgFfirst</guid>
      <pubDate>Tue, 03 Nov 2026 15:00:03 GMT</pubDate>
      <description>&lt;a href="https://news.google.com/rss/articles/first-story?oc=5" target="_blank"&gt;Grand Theft Auto VI Pre-Load Begins on November 10&lt;/a&gt;&amp;nbsp;&amp;nbsp;&lt;font color="#6f6f6f"&gt;Rockstar Games&lt;/font&gt;</description>
      <source url="https://www.rockstargames.com">Rockstar Games</source>
    </item>
  </channel>
</rss>
"""

class FeedParserTest {
    @Test
    fun rssValidFixture_allThreeItemsInDocumentOrder() {
        val items = parseFeed(RSS_VALID)
        assertEquals(3, items.size)
        assertEquals("First story with an enclosure image", items[0].title)
        assertEquals("https://example.com/articles/first-story", items[0].link)
        assertEquals("Second story with a media:content image", items[1].title)
        assertEquals("Third story with no image", items[2].title)
    }

    @Test
    fun rssEnclosureImage_winsAndIsTheRawUrl() {
        val items = parseFeed(RSS_VALID)
        assertEquals("https://example.com/images/first-story.jpg", items[0].imageUrl)
    }

    @Test
    fun rssMediaContentImage_usedWhenNoEnclosure() {
        val items = parseFeed(RSS_VALID)
        assertEquals("https://example.com/images/second-story.png", items[1].imageUrl)
    }

    @Test
    fun rssItemWithNeitherEnclosureNorMediaTag_hasNoImageUrl() {
        val items = parseFeed(RSS_VALID)
        assertNull(items[2].imageUrl)
    }

    @Test
    fun rssPubDate_rfc822IsNormalizedToInstant() {
        val items = parseFeed(RSS_VALID)
        assertEquals(Instant.parse("2026-11-03T09:00:00Z"), items[0].publishedAt)
    }

    @Test
    fun rssCdataHtmlDescription_becomesAShortPlainTextExcerpt() {
        val items = parseFeed(RSS_VALID)
        assertEquals("A short plain summary of the first story.", items[0].excerpt)
    }

    @Test
    fun rssRawNonCdataHtmlDescription_stillYieldsANonEmptyExcerpt() {
        val items = parseFeed(RSS_RAW_HTML_DESCRIPTION)
        assertEquals(1, items.size)
        assertEquals(
            "Grand Theft Auto VI Pre-Load Begins on November 10 Rockstar Games",
            items[0].excerpt,
        )
        assertNull(items[0].imageUrl)
    }

    @Test
    fun rssDoubleEscapedDescription_realGoogleNewsConvention_decodesBothLevelsOfEscaping() {
        val items = parseFeed(RSS_DOUBLE_ESCAPED_DESCRIPTION)
        assertEquals(1, items.size)
        assertEquals(
            "Grand Theft Auto VI Pre-Load Begins on November 10 Rockstar Games",
            items[0].excerpt,
        )
    }

    @Test
    fun atomValidFixture_bothEntries() {
        val items = parseFeed(ATOM_VALID)
        assertEquals(2, items.size)
        assertEquals("Atom story with alternate link and thumbnail", items[0].title)
    }

    @Test
    fun atomPrefersRelAlternate_overRelSelf() {
        val items = parseFeed(ATOM_VALID)
        assertEquals("https://example.com/articles/atom-story-1", items[0].link)
    }

    @Test
    fun atomFallsBackToTheOnlyLink_whenRelIsOmitted() {
        val items = parseFeed(ATOM_VALID)
        assertEquals("https://example.com/articles/atom-story-2", items[1].link)
    }

    @Test
    fun atomMediaThumbnail_becomesImageUrl() {
        val items = parseFeed(ATOM_VALID)
        assertEquals("https://example.com/images/atom-story-1.jpg", items[0].imageUrl)
    }

    @Test
    fun atomPrefersUpdated_overPublished() {
        val items = parseFeed(ATOM_VALID)
        assertEquals(Instant.parse("2026-11-03T08:45:00Z"), items[0].publishedAt)
    }

    @Test
    fun atomHtmlEscapedContent_strippedAndUnescapedInTheExcerpt() {
        val items = parseFeed(ATOM_VALID)
        assertEquals("HTML-escaped content body.", items[1].excerpt)
    }

    @Test
    fun malformedXml_throwsFeedParseException() {
        assertFailsWith<FeedParseException> { parseFeed(RSS_MALFORMED) }
    }

    @Test
    fun aFeedThatsNeitherRssNorAtom_throwsFeedParseException() {
        assertFailsWith<FeedParseException> { parseFeed("<somethingElse><a/></somethingElse>") }
    }

    @Test
    fun anRssItemMissingATitleOrLink_isSkippedNotThrown() {
        val xml = """<?xml version="1.0"?><rss version="2.0"><channel>
            <item><link>https://example.com/no-title</link></item>
            <item><title>No link here</title></item>
            <item><title>Valid</title><link>https://example.com/valid</link></item>
        </channel></rss>"""
        val items = parseFeed(xml)
        assertEquals(1, items.size)
        assertEquals("Valid", items[0].title)
    }

    @Test
    fun anEmptyString_isRejectedAsFeedParseException() {
        assertFailsWith<FeedParseException> { parseFeed("") }
    }

    @Test
    fun aLongNestedEntityLadenHtmlDescription_isTruncatedToAShortPlainExcerpt() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <item>
      <title>A story with a long, nested, entity-laden HTML description</title>
      <link>https://example.com/articles/html-heavy</link>
      <pubDate>Mon, 03 Nov 2026 10:00:00 GMT</pubDate>
      <description><![CDATA[
        <div class="summary">
          <p>This is a <strong>long</strong> description that deliberately runs well past the two
          hundred character excerpt limit this job enforces, so the truncation logic has something
          real to cut down to size instead of a description that was already short enough to pass
          through untouched &mdash; padding it out further here.</p>
          <img src="https://example.com/images/html-heavy.jpg" alt="inline image, not the excerpt image" />
          <p>A second paragraph that should never appear in the excerpt at all.</p>
        </div>
      ]]></description>
    </item>
  </channel>
</rss>"""
        val items = parseFeed(xml)
        assertEquals(1, items.size)
        val excerpt = requireNotNull(items[0].excerpt)
        assertTrue(excerpt.length <= 201, "expected <=201 chars, was ${excerpt.length}")
        assertTrue(excerpt.none { it == '<' || it == '>' }, "excerpt must not contain HTML tags")
        assertTrue(
            !excerpt.contains("second paragraph"),
            "excerpt must not reach the second paragraph"
        )
    }
}
