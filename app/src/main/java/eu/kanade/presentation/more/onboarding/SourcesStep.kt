package eu.kanade.presentation.more.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.AuroraSurfaceLevel
import eu.kanade.presentation.theme.AuroraTheme
import eu.kanade.presentation.theme.auroraFloatingSurface
import eu.kanade.presentation.theme.resolveAuroraBorderColor
import eu.kanade.presentation.theme.resolveAuroraSelectionBorderColor
import eu.kanade.presentation.theme.resolveAuroraSelectionContainerColor
import eu.kanade.presentation.theme.resolveAuroraSurfaceColor
import mihon.domain.extensionrepo.anime.interactor.CreateAnimeExtensionRepo
import mihon.domain.extensionrepo.manga.interactor.CreateMangaExtensionRepo
import mihon.domain.extensionrepo.novel.interactor.CreateNovelExtensionRepo
import org.json.JSONArray
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class SourcesStep : OnboardingStep {

    private val uiPreferences: UiPreferences = Injekt.get()

    private val createAnimeRepo: CreateAnimeExtensionRepo = Injekt.get()
    private val createMangaRepo: CreateMangaExtensionRepo = Injekt.get()
    private val createNovelRepo: CreateNovelExtensionRepo = Injekt.get()

    private var _isComplete by mutableStateOf(true)

    override val isComplete: Boolean
        get() = _isComplete

    val selectedRepos = mutableStateListOf<PredefinedRepo>()
    var isRegistering by mutableStateOf(false)
        private set

    data class PredefinedRepo(
        val name: String,
        val url: String,
        val type: String,
    )

    private fun loadPredefinedRepos(context: Context): List<PredefinedRepo> {
        val jsonString = try {
            context.assets.open("predefined_repos_private.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            try {
                context.assets.open("predefined_repos.json").bufferedReader().use { it.readText() }
            } catch (e2: Exception) {
                "[]"
            }
        }
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<PredefinedRepo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PredefinedRepo(
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        type = obj.getString("type"),
                    ),
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun registerSelectedRepos() {
        if (selectedRepos.isEmpty()) return
        isRegistering = true
        _isComplete = false
        try {
            selectedRepos.forEach { repo ->
                try {
                    when (repo.type) {
                        "anime" -> createAnimeRepo.await(
                            repo.url,
                            displayName = repo.name,
                            forceLocalInsert = true,
                        )
                        "manga" -> createMangaRepo.await(
                            repo.url,
                            displayName = repo.name,
                            forceLocalInsert = true,
                        )
                        "novel" -> createNovelRepo.await(
                            repo.url,
                            displayName = repo.name,
                            forceLocalInsert = true,
                        )
                    }
                } catch (e: Exception) {
                    // Ignore failures, proceed
                }
            }
        } finally {
            isRegistering = false
            _isComplete = true
        }
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val showAnime by uiPreferences.showAnimeSection().collectAsState()
        val showManga by uiPreferences.showMangaSection().collectAsState()
        val showNovel by uiPreferences.showNovelSection().collectAsState()

        val allRepos = remember { loadPredefinedRepos(context) }
        val filteredRepos = remember(showAnime, showManga, showNovel, allRepos) {
            allRepos.filter { repo ->
                (repo.type == "anime" && showAnime) ||
                    (repo.type == "manga" && showManga) ||
                    (repo.type == "novel" && showNovel)
            }
        }

        // Sync initially to select all filtered repos
        LaunchedEffect(filteredRepos) {
            selectedRepos.clear()
            selectedRepos.addAll(filteredRepos)
        }

        val colors = AuroraTheme.colors

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(MR.strings.onboarding_sources_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )

            Text(
                text = stringResource(MR.strings.onboarding_sources_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            if (isRegistering) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .auroraFloatingSurface(colors, AuroraSurfaceLevel.Glass, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(resolveAuroraSurfaceColor(colors, AuroraSurfaceLevel.Glass))
                        .border(1.dp, resolveAuroraBorderColor(colors, false), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                        )
                        Text(
                            text = stringResource(MR.strings.onboarding_sources_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                if (filteredRepos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .auroraFloatingSurface(colors, AuroraSurfaceLevel.Glass, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(resolveAuroraSurfaceColor(colors, AuroraSurfaceLevel.Glass))
                            .border(1.dp, resolveAuroraBorderColor(colors, false), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(MR.strings.onboarding_sources_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        filteredRepos.forEach { repo ->
                            val isChecked = selectedRepos.contains(repo)
                            val baseBg = resolveAuroraSurfaceColor(colors, AuroraSurfaceLevel.Glass)
                            val selectedBg = resolveAuroraSelectionContainerColor(colors)
                            val bgAnim by animateColorAsState(
                                targetValue = if (isChecked) selectedBg else baseBg,
                                label = "sourceBg",
                            )

                            val baseBorder = resolveAuroraBorderColor(colors, false)
                            val selectedBorder = resolveAuroraSelectionBorderColor(colors)
                            val borderAnim by animateColorAsState(
                                targetValue = if (isChecked) selectedBorder else baseBorder,
                                label = "sourceBorder",
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .auroraFloatingSurface(colors, AuroraSurfaceLevel.Glass, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgAnim)
                                    .border(1.dp, borderAnim, RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (isChecked) {
                                            selectedRepos.remove(repo)
                                        } else {
                                            selectedRepos.add(repo)
                                        }
                                    }
                                    .padding(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (isChecked) {
                                                selectedRepos.remove(repo)
                                            } else {
                                                selectedRepos.add(repo)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colors.accent,
                                            uncheckedColor = colors.textSecondary,
                                            checkmarkColor = colors.textOnAccent,
                                        ),
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = repo.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = repo.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Beautiful informative tip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.08f))
                        .border(1.dp, colors.accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(MR.strings.onboarding_sources_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}
