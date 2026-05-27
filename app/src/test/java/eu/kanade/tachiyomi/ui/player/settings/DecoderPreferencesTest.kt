package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.Anime4KShaderPreset
import eu.kanade.tachiyomi.ui.player.MotionInterpolationMode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DecoderPreferencesTest {

    @Test
    fun `anime4k preset defaults to off`() {
        val prefs = DecoderPreferences(InMemoryPreferenceStore())

        prefs.anime4kShaderPreset().get() shouldBe Anime4KShaderPreset.Off
    }

    @Test
    fun `motion interpolation defaults to off`() {
        val prefs = DecoderPreferences(InMemoryPreferenceStore())

        prefs.motionInterpolationMode().get() shouldBe MotionInterpolationMode.Off
    }
}
