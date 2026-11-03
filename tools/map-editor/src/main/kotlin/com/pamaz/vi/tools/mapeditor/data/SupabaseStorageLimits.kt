package com.pamaz.vi.tools.mapeditor.data

/**
 * Supabase Storage's current per-plan limits, checked against `supabase.com/pricing` and
 * `supabase.com/docs/guides/storage/uploads/file-limits` on 2026-11-01 — update these two
 * constants (and this comment's date) if the plan ever changes. Used by [MapPublishPlanning] to
 * warn before a new map version's estimated tile-pyramid size would meaningfully eat into (or
 * exceed) the project's included storage.
 *
 * Only the Free plan's total matters as a hard "this won't even fit" check in practice: this
 * project's Supabase instance is on the Free plan as of this writing (`backend/README.md`), and
 * [FREE_PLAN_TOTAL_STORAGE_BYTES] is small enough that a single large map version's tiles can
 * plausibly approach or exceed it, unlike [PRO_PLAN_TOTAL_STORAGE_BYTES].
 */
object SupabaseStorageLimits {
    /** Free plan: 1 GB of file storage included, total, across every bucket. */
    const val FREE_PLAN_TOTAL_STORAGE_BYTES: Long = 1L * 1024 * 1024 * 1024

    /** Free plan: a single object upload can't exceed 50 MB (irrelevant for tiles themselves, which are tiny, but relevant for the raw source image upload). */
    const val FREE_PLAN_MAX_FILE_BYTES: Long = 50L * 1024 * 1024

    /** Pro/Team plan: 100 GB of file storage included (then metered overage). */
    const val PRO_PLAN_TOTAL_STORAGE_BYTES: Long = 100L * 1024 * 1024 * 1024

    /** Warn once an estimated upload would use more than this fraction of the Free plan's total storage. */
    const val WARNING_THRESHOLD_FRACTION: Double = 0.5
}
