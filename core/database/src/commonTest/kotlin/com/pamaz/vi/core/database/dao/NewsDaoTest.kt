package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.testDatabase
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsItem
import com.pamaz.vi.core.model.NewsItemId
import com.pamaz.vi.core.model.NewsTopic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NewsDaoTest {
    private val base = Instant.parse("2026-01-01T00:00:00Z")

    private fun item(
        id: String,
        publishedAtOffsetSeconds: Long,
        topic: NewsTopic = NewsTopic.NEWS
    ) = NewsItem(
        id = NewsItemId(id),
        feedId = FeedSourceId("feed1"),
        title = "Item $id",
        link = "https://example.com/$id",
        publishedAt = base + publishedAtOffsetSeconds.seconds,
        topic = topic,
        fetchedAt = base,
    )

    @Test
    fun itemsOrderedByPublishedAtDescWithLimit() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        dao.upsertNewsItem(item("n1", 0))
        dao.upsertNewsItem(item("n2", 10))
        dao.upsertNewsItem(item("n3", 5))

        val items = dao.newsItems(NewsTopic.NEWS, limit = 2).first()
        assertEquals(listOf("n2", "n3"), items.map { it.id.value })
    }

    @Test
    fun itemsScopedByTopic() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        dao.upsertNewsItem(item("n1", 0, NewsTopic.NEWS))
        dao.upsertNewsItem(item("n2", 0, NewsTopic.MISSIONS))

        assertEquals(listOf("n1"), dao.newsItems(NewsTopic.NEWS).first().map { it.id.value })
        assertEquals(listOf("n2"), dao.newsItems(NewsTopic.MISSIONS).first().map { it.id.value })
    }

    @Test
    fun trimToLatestKeepsOnlyNewest() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        repeat(205) { i -> dao.upsertNewsItem(item("n$i", i.toLong())) }

        dao.trimToLatest(NewsTopic.NEWS, keep = 200)

        val remaining = dao.newsItems(NewsTopic.NEWS, limit = 1000).first()
        assertEquals(200, remaining.size)
        assertEquals("n204", remaining.first().id.value)
        assertEquals("n5", remaining.last().id.value)
    }
}
