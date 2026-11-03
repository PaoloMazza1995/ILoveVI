package com.pamaz.vi.core.data.news

import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.NewsItem
import com.pamaz.vi.core.model.NewsItemId
import kotlin.time.Clock

/** The newest items kept per topic on every sync pass — ported from the project's earlier TypeScript `fetch-news` job's `trim.ts`. */
internal const val MAX_ITEMS_PER_TOPIC = 200

/**
 * Collapses whitespace/case/punctuation so two stories that are really the same ("GTA 6 delayed
 * again!" vs. "GTA 6 Delayed Again") compare equal — deliberately generic (not source-specific
 * suffix-stripping, e.g. Google News' own "Title - Outlet" convention), since this is written
 * against RSS/Atom in general, not one particular aggregator's quirks. Unlike the TypeScript
 * version this ported from, accented letters aren't folded to their base letter first (no
 * `java.text.Normalizer`-equivalent in `commonMain`) — they're simply dropped by the
 * non-alphanumeric strip below, which still groups near-duplicates together for this heuristic's
 * purposes, just not byte-for-byte identically.
 */
internal fun normalizeTitle(title: String): String =
    title.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Tags each feed's items with that feed's id/topic, assigns the final [NewsItemId] (stable,
 * deterministic: `"<feedId>|<link>"` — this is a purely local, single-writer cache, so there's no
 * need for the backend version's SHA-256 hash, just a value that's unique and doesn't change
 * across runs), and de-duplicates — first by [ParsedFeedItem.link] (two feeds pointing at the
 * exact same URL; also required so a single `upsertNewsItem` call per item never collides with
 * itself), then by [normalizeTitle] (the same story syndicated under two different URLs). First
 * occurrence wins in both passes — [feedResults] is walked in its own order, so whichever feed is
 * listed first in `feed_sources` (ties broken by insertion order) is the kept copy.
 */
internal fun mergeAndDedupe(feedResults: List<Pair<FeedSource, List<ParsedFeedItem>>>): List<NewsItem> {
    val seenLinks = HashSet<String>()
    val seenTitles = HashSet<String>()
    val merged = mutableListOf<NewsItem>()
    val now = Clock.System.now()

    for ((feed, items) in feedResults) {
        for (item in items) {
            if (!seenLinks.add(item.link)) continue
            val normalizedTitle = normalizeTitle(item.title)
            if (normalizedTitle.isNotEmpty() && !seenTitles.add(normalizedTitle)) continue

            merged.add(
                NewsItem(
                    id = NewsItemId("${feed.id.value}|${item.link}"),
                    feedId = feed.id,
                    title = item.title,
                    link = item.link,
                    publishedAt = item.publishedAt,
                    excerpt = item.excerpt,
                    imageUrl = item.imageUrl,
                    topic = feed.topic,
                    fetchedAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
    return merged
}
