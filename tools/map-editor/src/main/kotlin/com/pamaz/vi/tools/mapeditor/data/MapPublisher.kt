package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.tools.mapeditor.model.MapPublishOutcome
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlan
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlanResult
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepState
import java.io.File

/**
 * PLT-08: validates a chosen image and computes its tile-pyramid plan ([plan]), then runs the
 * generate/upload/verify/register pipeline for it ([publish]). One instance is reused across a
 * Retry within the same dialog session, so it can skip whatever [publish] already finished last
 * time instead of starting over — see the real implementation
 * (`data/supabase/SupabaseMapPublisher.kt`) for exactly what "already finished" tracks.
 */
interface MapPublisher {
    suspend fun plan(file: File): MapPublishPlanResult

    /** Reports incremental progress via [onProgress] as each step runs; safe to call again after a [MapPublishOutcome.Failure] to resume. */
    suspend fun publish(
        plan: MapPublishPlan,
        onProgress: (List<MapPublishStepState>) -> Unit
    ): MapPublishOutcome
}
