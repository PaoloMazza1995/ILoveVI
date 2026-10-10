package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Which section of the News destination an item belongs to (functional analysis v0.2). */
@Serializable
enum class NewsTopic { NEWS, MISSIONS }

/** Typed id of a [FeedSource]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class FeedSourceId(val value: String)

/** One RSS/Atom feed the app polls for [NewsItem]s. */
@Serializable
data class FeedSource(
    val id: FeedSourceId,
    val name: String,
    val url: String,
    val topic: NewsTopic,
    /** Whether this feed is currently polled. Defaults to on for a newly added source. */
    val enabled: Boolean = true,
    /** Last time this feed's data changed, used for delta sync. */
    val updatedAt: kotlin.time.Instant,
)

/** Typed id of a [NewsItem]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class NewsItemId(val value: String)

/**
 * One story fetched from a [FeedSource]. [id] is expected to be the feed's own guid, or a hash
 * of [link] when the feed has none — both derivations are the data layer's concern, not this
 * module's.
 */
@Serializable
data class NewsItem(
    val id: NewsItemId,
    val feedId: FeedSourceId,
    /** Headline, as published by the source: not translated in this model. */
    val title: String,
    /** The original article's URL. */
    val link: String,
    val publishedAt: kotlin.time.Instant,
    /** Short teaser, never the full article body. */
    val excerpt: String? = null,
    val imageUrl: String? = null,
    val topic: NewsTopic,
    /** When this app's cache last fetched the item — bookkeeping for delta sync, distinct from [publishedAt]. */
    val fetchedAt: kotlin.time.Instant,
)
