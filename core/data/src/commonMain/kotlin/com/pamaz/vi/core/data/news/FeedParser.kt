package com.pamaz.vi.core.data.news

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * One story as parsed from a feed, before it's tagged with a [com.pamaz.vi.core.model.FeedSource]
 * and turned into a [com.pamaz.vi.core.model.NewsItem] — mirrors the project's earlier TypeScript
 * `fetch-news` job's own `ParsedItem`: title, link, date, a short excerpt, an image url. Never the
 * full article body.
 */
internal data class ParsedFeedItem(
    val title: String,
    val link: String,
    val publishedAt: Instant,
    val excerpt: String?,
    val imageUrl: String?,
)

internal class FeedParseException(message: String) : Exception(message)

/** Parses [xml] as either RSS 2.0 or Atom (auto-detected from the root element) into a flat, normalized item list. */
internal fun parseFeed(xml: String): List<ParsedFeedItem> {
    val root = try {
        parseXmlDocument(xml)
    } catch (e: XmlParseException) {
        throw FeedParseException("not valid XML: ${e.message}")
    }
    return when (root.name) {
        "rss" -> parseRss(root)
        "feed" -> parseAtom(root)
        else -> throw FeedParseException("neither an <rss> nor an <feed> (Atom) root element found")
    }
}

// --- RSS 2.0 -------------------------------------------------------------------------------

private fun parseRss(rss: XmlElement): List<ParsedFeedItem> {
    val channel = rss.child("channel") ?: throw FeedParseException("<rss> has no <channel>")
    return channel.children("item").mapNotNull(::parseRssItem)
}

private fun parseRssItem(item: XmlElement): ParsedFeedItem? {
    val title = item.child("title")?.directText() ?: return null
    val link = item.child("link")?.directText()
        ?: return null // task's own "malformed item": skip, don't crash the run.

    val pubDateText = item.child("pubDate")?.directText() ?: item.child("dc:date")?.directText()
    val publishedAt = parseFeedDate(pubDateText) ?: Clock.System.now()

    val description = item.child("description")?.allText()?.takeUnless { it.isBlank() }
        ?: item.child("content:encoded")?.allText()

    return ParsedFeedItem(
        title = title,
        link = link,
        publishedAt = publishedAt,
        excerpt = toExcerpt(description),
        imageUrl = extractImage(item),
    )
}

// --- Atom ------------------------------------------------------------------------------------

private fun parseAtom(feed: XmlElement): List<ParsedFeedItem> =
    feed.children("entry").mapNotNull(::parseAtomEntry)

private fun parseAtomEntry(entry: XmlElement): ParsedFeedItem? {
    val title = entry.child("title")?.directText() ?: return null
    val link = atomLink(entry.children("link")) ?: return null

    val dateText = entry.child("updated")?.directText() ?: entry.child("published")?.directText()
    val publishedAt = parseFeedDate(dateText) ?: Clock.System.now()

    val summary = entry.child("summary")?.allText()?.takeUnless { it.isBlank() }
        ?: entry.child("content")?.allText()

    return ParsedFeedItem(
        title = title,
        link = link,
        publishedAt = publishedAt,
        excerpt = toExcerpt(summary),
        imageUrl = extractImage(entry),
    )
}

/** Atom's `<link>` can be one element or several (different `rel`s); prefers `rel="alternate"` (the article page) — the Atom spec's own default when `rel` is omitted. */
private fun atomLink(links: List<XmlElement>): String? {
    val alternate = links.firstOrNull { link ->
        val rel = link.attr("rel")
        rel == null || rel == "alternate"
    }
    return (alternate ?: links.firstOrNull())?.attr("href")
}

// --- image extraction (shared by RSS and Atom; both can carry media: namespace elements) -------

/** Priority: `<enclosure>` (RSS), then `media:content`, then `media:thumbnail`. Only an enclosure whose MIME type starts with "image/" counts; a podcast's audio enclosure must not become [ParsedFeedItem.imageUrl]. */
private fun extractImage(entryOrItem: XmlElement): String? {
    val enclosureImage = entryOrItem.children("enclosure")
        .firstOrNull { (it.attr("type") ?: "").startsWith("image/") }

    val mediaContentImage = entryOrItem.children("media:content").firstOrNull { media ->
        val type = media.attr("type") ?: ""
        val medium = media.attr("medium") ?: ""
        type.startsWith("image/") || medium == "image" || (type.isEmpty() && medium.isEmpty())
    }

    val thumbnail = entryOrItem.children("media:thumbnail").firstOrNull()

    return enclosureImage?.attr("url") ?: mediaContentImage?.attr("url") ?: thumbnail?.attr("url")
}
