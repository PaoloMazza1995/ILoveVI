package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

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
    val updatedAt: Instant,
)

/** Typed id of a [NewsItem]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class NewsItemId(val value: String)

/**
 * One story fetched from a [FeedSource]. [id] is a stable id derived from `(feedId, link)` — the
 * data layer's concern, not this module's (see `:core:data`'s `mergeAndDedupe`).
 *
 * A device-local cache, per the functional analysis' data model (section 8): there is no backend
 * `news_items` table. `ContentSyncer.syncNews()` (`:core:data`) fetches and parses each enabled
 * [FeedSource]'s RSS/Atom URL directly, on-device, and this is what gets cached locally — never
 * synced from, or persisted to, a remote table the way every other content type in this model is.
 */
@Serializable
data class NewsItem(
    val id: NewsItemId,
    val feedId: FeedSourceId,
    /** Headline, as published by the source: not translated in this model. */
    val title: String,
    /** The original article's URL. */
    val link: String,
    val publishedAt: Instant,
    /** Short teaser, never the full article body. */
    val excerpt: String? = null,
    val imageUrl: String? = null,
    val topic: NewsTopic,
    /** When this device fetched this item from its RSS/Atom source — distinct from [updatedAt] (last local-row change) and [publishedAt] (the source's own publish date). */
    val fetchedAt: Instant,
    /** Last time this local row changed (upsert or tombstone) — device-local bookkeeping only, not a sync cursor (there's nothing remote to compare it against). */
    val updatedAt: Instant,
)
