package eu.kanade.presentation.entries.anime.components.aurora

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.AuroraTheme
import eu.kanade.tachiyomi.animesource.model.FetchType
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.pluralStringResource

/**
 * Header for episodes or seasons section in Aurora theme.
 */
@Composable
fun EpisodesHeader(
    itemCount: Int,
    fetchType: FetchType = FetchType.Episodes,
    modifier: Modifier = Modifier,
) {
    val colors = AuroraTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = when (fetchType) {
                FetchType.Seasons -> pluralStringResource(AYMR.plurals.anime_num_seasons, count = itemCount, itemCount)
                FetchType.Episodes -> pluralStringResource(
                    AYMR.plurals.anime_num_episodes,
                    count = itemCount,
                    itemCount,
                )
            },
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = colors.divider,
            thickness = 1.dp,
        )
    }
}
