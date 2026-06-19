package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Event Horizon.
 *
 * Aurora-exclusive "singularity UI" palette. Unlike the other reward themes it
 * avoids another colorful accent-first look: dark mode is built around x-ray
 * white controls on near-black surfaces, infrared error/energy bands, and a
 * cold green lensing accent. Light mode becomes a paper observatory variant.
 */
internal object EventHorizonColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFF8F3E6),
        onPrimary = Color(0xFF070407),
        primaryContainer = Color(0xFF28222A),
        onPrimaryContainer = Color(0xFFFFF8EF),
        inversePrimary = Color(0xFF4D4651),

        secondary = Color(0xFFFF4D2E),
        onSecondary = Color(0xFF230400),
        secondaryContainer = Color(0xFF5A1209),
        onSecondaryContainer = Color(0xFFFFDAD2),

        tertiary = Color(0xFFB7FF6A),
        onTertiary = Color(0xFF112100),
        tertiaryContainer = Color(0xFF263F00),
        onTertiaryContainer = Color(0xFFD8FFA4),

        background = Color(0xFF020103),
        onBackground = Color(0xFFF8F3E6),
        surface = Color(0xFF070407),
        onSurface = Color(0xFFF8F3E6),
        surfaceVariant = Color(0xFF1E1A22),
        onSurfaceVariant = Color(0xFFD2CAD6),
        surfaceTint = Color(0xFFF8F3E6),
        inverseSurface = Color(0xFFF8F3E6),
        inverseOnSurface = Color(0xFF201A22),

        outline = Color(0xFFA89DAE),
        outlineVariant = Color(0xFF3E3544),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color(0xFF000000),

        surfaceDim = Color(0xFF020103),
        surfaceBright = Color(0xFF302A33),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF09050A),
        surfaceContainer = Color(0xFF0F0A12),
        surfaceContainerHigh = Color(0xFF19121D),
        surfaceContainerHighest = Color(0xFF241B2A),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF242027),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8DFEA),
        onPrimaryContainer = Color(0xFF1E1821),
        inversePrimary = Color(0xFFF8F3E6),

        secondary = Color(0xFFA72A13),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDAD2),
        onSecondaryContainer = Color(0xFF3A0700),

        tertiary = Color(0xFF466800),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD8FFA4),
        onTertiaryContainer = Color(0xFF112100),

        background = Color(0xFFFCF7EF),
        onBackground = Color(0xFF211A1F),
        surface = Color(0xFFFCF7EF),
        onSurface = Color(0xFF211A1F),
        surfaceVariant = Color(0xFFE8DFEA),
        onSurfaceVariant = Color(0xFF504751),
        surfaceTint = Color(0xFF242027),
        inverseSurface = Color(0xFF362F36),
        inverseOnSurface = Color(0xFFF7EEF6),

        outline = Color(0xFF827783),
        outlineVariant = Color(0xFFD2C6D3),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        scrim = Color(0xFF000000),

        surfaceDim = Color(0xFFDED7DF),
        surfaceBright = Color(0xFFFCF7EF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F0F7),
        surfaceContainer = Color(0xFFF1EAF1),
        surfaceContainerHigh = Color(0xFFEBE4EC),
        surfaceContainerHighest = Color(0xFFE5DEE6),
    )
}
