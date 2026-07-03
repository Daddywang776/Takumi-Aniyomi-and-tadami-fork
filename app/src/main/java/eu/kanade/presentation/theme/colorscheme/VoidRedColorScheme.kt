package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Void Red (Terminal Red) theme.
 * High-contrast dark red/crimson digital terminal.
 */
internal object VoidRedColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFF003C), // Crimson Red
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF5A0012),
        onPrimaryContainer = Color(0xFFFFDADE),
        inversePrimary = Color(0xFFC0002A),

        secondary = Color(0xFFD30030),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF3C000C),
        onSecondaryContainer = Color(0xFFFFE6E8),

        tertiary = Color(0xFFFF8B9F),
        onTertiary = Color(0xFF5F0018),
        tertiaryContainer = Color(0xFF8B002A),
        onTertiaryContainer = Color(0xFFFFDADE),

        background = Color(0xFF000000), // Pitch Black AMOLED
        onBackground = Color(0xFFECEFF1), // Off-white
        surface = Color(0xFF080808),
        onSurface = Color(0xFFECEFF1),
        surfaceVariant = Color(0xFF161616),
        onSurfaceVariant = Color(0xFFCFD8DC),
        surfaceTint = Color(0xFFFF003C),
        inverseSurface = Color(0xFFECEFF1),
        inverseOnSurface = Color(0xFF101010),

        outline = Color(0xFF8B0018),
        outlineVariant = Color(0xFF3A000A),

        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),

        scrim = Color(0xFF000000),

        surfaceDim = Color(0xFF040404),
        surfaceBright = Color(0xFF221113),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0C0C0C),
        surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF1A1A1A),
        surfaceContainerHighest = Color(0xFF242424),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFC0002A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDADE),
        onPrimaryContainer = Color(0xFF40000B),
        inversePrimary = Color(0xFFFFB4AB),

        secondary = Color(0xFF8F001D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDADE),
        onSecondaryContainer = Color(0xFF300005),

        tertiary = Color(0xFFB90035),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8DC),
        onTertiaryContainer = Color(0xFF40000D),

        background = Color(0xFFFFF8F8),
        onBackground = Color(0xFF211A1B),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF211A1B),
        surfaceVariant = Color(0xFFF4E0E2),
        onSurfaceVariant = Color(0xFF534344),
        surfaceTint = Color(0xFFC0002A),
        inverseSurface = Color(0xFF362E2F),
        inverseOnSurface = Color(0xFFFBEEEE),

        outline = Color(0xFF857375),
        outlineVariant = Color(0xFFD8C2C4),

        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),

        scrim = Color(0xFF000000),

        surfaceDim = Color(0xFFE8D7D8),
        surfaceBright = Color(0xFFFFF8F8),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFDF1F2),
        surfaceContainer = Color(0xFFF7E6E7),
        surfaceContainerHigh = Color(0xFFF1DCDE),
        surfaceContainerHighest = Color(0xFFEBD2D4),
    )
}
