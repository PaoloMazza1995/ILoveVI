package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.visibleTiles
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * **What this test does and does not prove.** It measures the pure-Kotlin cost of the math every
 * frame needs — [TileScheme.visibleTiles] plus [MapViewport.toScreen] + viewport culling for 1000
 * pins — repeated many times, on the JVM, in this headless CI/sandboxed environment. That is the
 * only part of "stays smooth with 1000 pins" this environment can actually measure: there is no
 * display here, no Android/iOS device or emulator, and no Compose UI test harness wired up in this
 * module, so an actual rendered-frame-time measurement could not be produced and is not claimed —
 * see the module README for what still needs doing on a real device/emulator before relying on
 * this number.
 *
 * The threshold is deliberately generous (a small fraction of a 16ms/60fps frame budget): it's a
 * regression guard against an accidentally-quadratic change to the pin/tile math, not a tuned
 * performance target.
 */
class MapViewerPerfTest {

    private val scheme = TileScheme(widthPx = 8192, heightPx = 6144)

    private fun fakePins(count: Int): List<MapViewerPin> {
        val random = Random(42)
        val categories = EntryCategory.entries
        return List(count) { index ->
            MapViewerPin(
                id = "pin-$index",
                position = NormalizedPoint(random.nextFloat(), random.nextFloat()),
                category = categories[index % categories.size],
            )
        }
    }

    /** What [PinLayer] does once per frame: compute each pin's screen position and cull off-screen ones. */
    private fun simulateOneFramePinLayout(
        pins: List<MapViewerPin>,
        center: NormalizedPoint,
        zoom: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): Int {
        var visibleCount = 0
        for (pin in pins) {
            val screen = MapViewport.toScreen(
                pin.position,
                scheme,
                center,
                zoom,
                viewportWidthPx,
                viewportHeightPx
            )
            val visible =
                screen.x in -44f..(viewportWidthPx + 44f) && screen.y in -44f..(viewportHeightPx + 44f)
            if (visible) visibleCount++
        }
        return visibleCount
    }

    @Test
    fun perFrameComputation_with1000Pins_staysWellUnderFrameBudget() {
        val pins = fakePins(1000)
        val frameCount = 300
        val random = Random(7)

        val elapsed = measureTime {
            repeat(frameCount) {
                // A representative pan/zoom sweep, not a fixed camera, so the culling branch is exercised too.
                val center = NormalizedPoint(random.nextFloat(), random.nextFloat())
                val zoom = scheme.minZoom + random.nextFloat() * (scheme.maxZoom - scheme.minZoom)
                scheme.visibleTiles(center, zoom, 1080f, 2000f)
                simulateOneFramePinLayout(pins, center, zoom, 1080f, 2000f)
            }
        }

        val averageMillisPerFrame = elapsed.inWholeMicroseconds / frameCount / 1000.0
        println(
            "MapViewerPerfTest: ${pins.size} pins, $frameCount simulated frames, " +
                    "total=${elapsed.inWholeMilliseconds}ms, average=${averageMillisPerFrame}ms/frame " +
                    "(pure tile+pin math only; see test KDoc)",
        )

        assertTrue(
            averageMillisPerFrame < 4.0,
            "expected average per-frame math cost under 4ms, was ${averageMillisPerFrame}ms",
        )
    }
}
