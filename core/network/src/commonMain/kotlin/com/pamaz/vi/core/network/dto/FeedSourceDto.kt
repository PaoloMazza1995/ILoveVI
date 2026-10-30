package com.pamaz.vi.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `feed_sources` row shape — `backend/supabase/migrations/20261011120005_feed_sources.sql`. */
@Serializable
internal data class FeedSourceDto(
    val id: String,
    val name: String,
    val url: String,
    val topic: String,
    val enabled: Boolean = true,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
