package com.pamaz.vi.core.data.news

import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsTopic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val now = Instant.parse("2026-01-01T00:00:00Z")

private fun feed(id: String, topic: NewsTopic = NewsTopic.NEWS) =
    FeedSource(FeedSourceId(id), "[$id]", "https://example.com/$id.xml", topic, true, now)

private fun parsed(title: String, link: String) =
    ParsedFeedItem(title, link, now, excerpt = null, imageUrl = null)

class FeedMergeTest {
    @Test
    fun normalizeTitle_caseAndPunctuationDifferencesCompareEqual() {
        assertEquals(normalizeTitle("GTA 6 delayed again!"), normalizeTitle("GTA 6 Delayed Again"))
    }

    @Test
    fun normalizeTitle_genuinelyDifferentTitlesStayDifferent() {
        assertEquals(
            false,
            normalizeTitle("GTA 6 release date") == normalizeTitle("GTA 6 price leak")
        )
    }

    @Test
    fun mergeAndDedupe_tagsEveryItemWithItsFeedsIdAndTopic() {
        val merged = mergeAndDedupe(
            listOf(
                feed("feed-1", NewsTopic.MISSIONS) to listOf(
                    parsed(
                        "Story",
                        "https://a.com/1"
                    )
                )
            ),
        )
        assertEquals(1, merged.size)
        assertEquals(FeedSourceId("feed-1"), merged[0].feedId)
        assertEquals(NewsTopic.MISSIONS, merged[0].topic)
        assertEquals("feed-1|https://a.com/1", merged[0].id.value)
    }

    @Test
    fun mergeAndDedupe_deduplicatesTheSameLinkAcrossTwoFeeds() {
        val merged = mergeAndDedupe(
            listOf(
                feed("feed-1") to listOf(parsed("Story", "https://a.com/1")),
                feed("feed-2") to listOf(parsed("Story (via mirror)", "https://a.com/1")),
            ),
        )
        assertEquals(1, merged.size)
        assertEquals(FeedSourceId("feed-1"), merged[0].feedId, "first occurrence wins")
    }

    @Test
    fun mergeAndDedupe_deduplicatesNearIdenticalTitlesWithDifferentLinks() {
        val merged = mergeAndDedupe(
            listOf(
                feed("feed-1") to listOf(parsed("GTA 6 delayed again!", "https://a.com/1")),
                feed("feed-2") to listOf(parsed("GTA 6 Delayed Again", "https://b.com/1")),
            ),
        )
        assertEquals(1, merged.size)
    }

    @Test
    fun mergeAndDedupe_differentStoriesFromDifferentFeedsAllSurvive() {
        val merged = mergeAndDedupe(
            listOf(
                feed("feed-1") to listOf(parsed("Story A", "https://a.com/1")),
                feed("feed-2") to listOf(parsed("Story B", "https://b.com/1")),
            ),
        )
        assertEquals(2, merged.size)
    }
}
