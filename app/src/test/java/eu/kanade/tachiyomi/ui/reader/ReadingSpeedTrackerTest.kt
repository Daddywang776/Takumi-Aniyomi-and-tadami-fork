package eu.kanade.tachiyomi.ui.reader

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReadingSpeedTrackerTest {

    @Test
    fun `should return null when there are fewer than 2 transitions`() {
        val tracker = ReadingSpeedTracker(maxHistorySize = 5, maxGapMs = 120_000)
        tracker.getAverageSpeedSeconds().shouldBeNull()

        tracker.addPageTransition(1000L)
        tracker.getAverageSpeedSeconds().shouldBeNull()
    }

    @Test
    fun `should calculate average speed from transitions`() {
        val tracker = ReadingSpeedTracker(maxHistorySize = 5, maxGapMs = 120_000)
        tracker.addPageTransition(1000L)
        tracker.addPageTransition(3000L) // 2s
        tracker.addPageTransition(7000L) // 4s

        // Average of 2s and 4s is 3s
        tracker.getAverageSpeedSeconds() shouldBe 3.0
    }

    @Test
    fun `should only keep up to max history transitions`() {
        val tracker = ReadingSpeedTracker(maxHistorySize = 2, maxGapMs = 120_000)
        tracker.addPageTransition(1000L)
        tracker.addPageTransition(3000L) // 2s
        tracker.addPageTransition(7000L) // 4s
        tracker.addPageTransition(12000L) // 5s

        // With maxHistorySize = 2, we only keep the last 2 transitions: (7000 to 12000 -> 5s) and (3000 to 7000 -> 4s)
        // Average of 4s and 5s is 4.5s
        tracker.getAverageSpeedSeconds() shouldBe 4.5
    }

    @Test
    fun `should ignore transitions exceeding maxGapMs`() {
        val tracker = ReadingSpeedTracker(maxHistorySize = 5, maxGapMs = 10_000)
        tracker.addPageTransition(1000L)
        tracker.addPageTransition(3000L) // 2s
        tracker.addPageTransition(20000L) // 17s (ignored!)
        tracker.addPageTransition(24000L) // 4s

        // The transition from 3000 to 20000 is 17s (exceeds 10s), so we ignore it.
        // The valid transitions are: 1000->3000 (2s) and 20000->24000 (4s).
        // Average of 2s and 4s is 3s.
        tracker.getAverageSpeedSeconds() shouldBe 3.0
    }
}
