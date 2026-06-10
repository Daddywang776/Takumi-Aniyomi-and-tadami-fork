@file:Suppress("ktlint:standard:filename")

package eu.kanade.presentation.components

import androidx.compose.ui.graphics.Color

internal data class AuraPalette(
    val id: String,
    val title: String,
    val description: String,
    val accentColor: Color,
    val gradientColors: List<Color>,
)

private val auraPalettes = listOf(
    AuraPalette(
        id = "aura_harem",
        title = "Гаремная аура",
        description = "Розовое свечение вокруг карточек контента",
        accentColor = Color(0xFFFF69B4),
        gradientColors = listOf(Color(0xFFFFB7D5), Color(0xFFFF69B4), Color(0xFFFF1493)),
    ),
    AuraPalette(
        id = "aura_matrix",
        title = "Цифровой дождь",
        description = "Неоновое зеленое свечение с цифровым настроением",
        accentColor = Color(0xFF00FF41),
        gradientColors = listOf(Color(0xFF00FF41), Color(0xFF008F11), Color(0xFF00FF41)),
    ),
    AuraPalette(
        id = "aura_level_up",
        title = "Аура мастера достижений",
        description = "Золотое сияние за прогресс по достижениям",
        accentColor = Color(0xFFFFD700),
        gradientColors = listOf(Color(0xFFFFE082), Color(0xFFFFD700), Color(0xFFFFB300)),
    ),
    AuraPalette(
        id = "aura_trinity_orbit",
        title = "Trinity Orbit",
        description = "Три орбиты манги, аниме и ранобэ вокруг карточек",
        accentColor = Color(0xFF9C7CFF),
        gradientColors = listOf(Color(0xFF64E8FF), Color(0xFF9C7CFF), Color(0xFFFFD36E)),
    ),
    AuraPalette(
        id = "aura_deep_focus",
        title = "Deep Focus",
        description = "Спокойное сине-бирюзовое свечение для длинных сессий",
        accentColor = Color(0xFF5DE7D8),
        gradientColors = listOf(Color(0xFF0B1028), Color(0xFF1F8EA3), Color(0xFF5DE7D8)),
    ),
    AuraPalette(
        id = "aura_shadow_monarch",
        title = "Shadow Monarch",
        description = "Фиолетово-чёрная мифическая тень",
        accentColor = Color(0xFFB36BFF),
        gradientColors = listOf(Color(0xFF13051F), Color(0xFF5D2A9D), Color(0xFFB36BFF)),
    ),
    AuraPalette(
        id = "aura_ascendant_gold",
        title = "Ascendant Gold",
        description = "Белое золото для рангового престижа",
        accentColor = Color(0xFFFFE08A),
        gradientColors = listOf(Color(0xFFFFF8D6), Color(0xFFFFD36E), Color(0xFFFFA726)),
    ),
)

private val auraPriority = listOf(
    "aura_shadow_monarch",
    "aura_ascendant_gold",
    "aura_trinity_orbit",
    "aura_deep_focus",
    "aura_harem",
    "aura_matrix",
    "aura_level_up",
)

internal fun resolveAuraPalette(id: String): AuraPalette? {
    return auraPalettes.firstOrNull { it.id == id }
}

internal fun resolveActiveAuraPalette(enabledAuras: Set<String>): AuraPalette? {
    val activeId = auraPriority.firstOrNull(enabledAuras::contains) ?: return null
    return resolveAuraPalette(activeId)
}

internal fun allAuraPalettes(): List<AuraPalette> = auraPalettes
