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
import kotlin.test.assertFalse
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
        updatedAt = base,
    )

    @Test
    fun itemsOrderedByPublishedAtDescWithLimit() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        dao.upsertNewsItem(item("n1", 0))
        dao.upsertNewsItem(item("n2", 10))
        dao.upsertNewsItem(item("n3", 5))

        val items = dao.newsItems(limit = 2).first()
        assertEquals(listOf("n2", "n3"), items.map { it.id.value })
    }

    @Test
    fun itemsMergedAcrossTopics() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        dao.upsertNewsItem(item("n1", 0, NewsTopic.NEWS))
        dao.upsertNewsItem(item("n2", 10, NewsTopic.MISSIONS))

        val items = dao.newsItems().first()
        assertEquals(listOf("n2", "n1"), items.map { it.id.value })
    }

    @Test
    fun tombstonedItemsAreExcluded() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        dao.upsertNewsItem(item("n1", 0))
        dao.upsertNewsItem(item("n2", 10))

        dao.tombstoneNewsItem(NewsItemId("n2"), deletedAt = base, updatedAt = base)

        val items = dao.newsItems().first()
        assertEquals(listOf("n1"), items.map { it.id.value })
        assertFalse(items.any { it.id.value == "n2" })
    }

    @Test
    fun upsertOverwritesExistingItem() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        dao.upsertNewsItem(item("n1", 0))
        dao.upsertNewsItem(item("n1", 0).copy(title = "Updated title"))

        val items = dao.newsItems().first()
        assertEquals("Updated title", items.single().title)
    }

    @Test
    fun trimNewsItems_keepsOnlyTheNewestPerTopic_leavingTheOtherTopicUntouched() = runTest {
        val dao = NewsDaoImpl(testDatabase())
        (0 until 5).forEach { i -> dao.upsertNewsItem(item("news-$i", i.toLong(), NewsTopic.NEWS)) }
        dao.upsertNewsItem(item("missions-1", 0, NewsTopic.MISSIONS))

        dao.trimNewsItems(NewsTopic.NEWS, keep = 2, deletedAt = base, updatedAt = base)

        val items = dao.newsItems().first()
        assertEquals(listOf("news-4", "news-3", "missions-1"), items.map { it.id.value })
    }
}
