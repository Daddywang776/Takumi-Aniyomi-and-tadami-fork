package eu.kanade.presentation.more.settings.screen.shikimori

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.settings.screen.anixart.AnixartImportScreenModel.SourceChoice
import eu.kanade.tachiyomi.data.anixart.AnixartSourceSearcher
import eu.kanade.tachiyomi.data.shikimori.FetchShikimoriImportEntries
import eu.kanade.tachiyomi.data.shikimori.ImportShikimoriEntries
import eu.kanade.tachiyomi.data.shikimori.ImportShikimoriMangaEntries
import eu.kanade.tachiyomi.data.shikimori.ImportShikimoriNovelEntries
import eu.kanade.tachiyomi.data.shikimori.ShikimoriMangaSourceSearcher
import eu.kanade.tachiyomi.data.shikimori.ShikimoriNovelSourceSearcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tachiyomi.data.anixart.AnixartMatcher
import tachiyomi.data.anixart.AnixartMatchingCoordinator
import tachiyomi.data.anixart.AnixartSourceHints
import tachiyomi.data.anixart.AnixartTitleSearcher
import tachiyomi.data.shikimori.ShikimoriImportEntry
import tachiyomi.data.shikimori.ShikimoriImportMediaType
import tachiyomi.data.shikimori.ShikimoriImportStatus
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.category.novel.interactor.GetNovelCategories
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.domain.source.novel.service.NovelSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ShikimoriImportScreenModel(
    private val animeSourceManager: AnimeSourceManager = Injekt.get(),
    private val mangaSourceManager: MangaSourceManager = Injekt.get(),
    private val novelSourceManager: NovelSourceManager = Injekt.get(),
    private val getAnimeCategories: GetAnimeCategories = Injekt.get(),
    private val getMangaCategories: GetMangaCategories = Injekt.get(),
    private val getNovelCategories: GetNovelCategories = Injekt.get(),
    private val fetchEntries: FetchShikimoriImportEntries = Injekt.get(),
    private val importAnimeEntries: ImportShikimoriEntries = Injekt.get(),
    private val importMangaEntries: ImportShikimoriMangaEntries = Injekt.get(),
    private val importNovelEntries: ImportShikimoriNovelEntries = Injekt.get(),
) : StateScreenModel<ShikimoriImportScreenModel.State>(State.Loading(ShikimoriImportMediaType.ANIME)) {

    @Immutable
    data class ReviewItem(
        val entry: ShikimoriImportEntry,
        val result: AnixartMatcher.MatchResult,
        val selectedId: Long?,
        val enabled: Boolean,
        val matchedQuery: String?,
        val matchedSourceName: String?,
    )

    @Immutable
    data class ImportReport(
        val added: Int,
        val alreadyInLibrary: Int,
        val failed: Int,
        val trackerBound: Int,
    )

    sealed interface State {
        val mediaType: ShikimoriImportMediaType

        data class Loading(override val mediaType: ShikimoriImportMediaType) : State
        data class Error(
            override val mediaType: ShikimoriImportMediaType,
            val messageKey: ErrorKind,
        ) : State
        data class PickSources(
            override val mediaType: ShikimoriImportMediaType,
            val entries: List<ShikimoriImportEntry>,
            val sources: List<SourceChoice>,
            val categories: List<CategoryUi>,
            val statusCategoryIds: Map<ShikimoriImportStatus, Long?>,
            val favoriteCategoryId: Long?,
            val statusFilter: Set<ShikimoriImportStatus>,
            val largeImport: Boolean,
        ) : State
        data class Matching(
            override val mediaType: ShikimoriImportMediaType,
            val current: Int,
            val total: Int,
        ) : State
        data class Review(
            override val mediaType: ShikimoriImportMediaType,
            val items: List<ReviewItem>,
            val matchingReport: AnixartMatchingCoordinator.MatchingReport,
            val statusCategoryIds: Map<ShikimoriImportStatus, Long?>,
            val favoriteCategoryId: Long?,
        ) : State
        data class Importing(
            override val mediaType: ShikimoriImportMediaType,
            val current: Int,
            val total: Int,
        ) : State
        data class Done(
            override val mediaType: ShikimoriImportMediaType,
            val report: ImportReport,
            val matchingReport: AnixartMatchingCoordinator.MatchingReport,
        ) : State
    }

    @Immutable
    data class CategoryUi(
        val id: Long,
        val name: String,
    )

    enum class ErrorKind { NOT_LOGGED_IN, EMPTY }

    init {
        load(ShikimoriImportMediaType.ANIME)
    }

    fun switchMediaType(mediaType: ShikimoriImportMediaType) {
        val current = state.value
        if (current.mediaType == mediaType) return
        if (current !is State.PickSources && current !is State.Loading && current !is State.Error) return
        load(mediaType)
    }

    private fun load(mediaType: ShikimoriImportMediaType) {
        mutableState.update { State.Loading(mediaType) }
        screenModelScope.launch {
            try {
                val entries = fetchEntries.await(mediaType)
                if (entries.isEmpty()) {
                    mutableState.update { State.Error(mediaType, ErrorKind.EMPTY) }
                    return@launch
                }
                val sources = catalogueSources(mediaType)
                val categories = loadCategories(mediaType)
                mutableState.update {
                    State.PickSources(
                        mediaType = mediaType,
                        entries = entries,
                        sources = sources,
                        categories = categories,
                        statusCategoryIds = emptyMap(),
                        favoriteCategoryId = null,
                        statusFilter = ShikimoriImportStatus.forMediaType(mediaType).toSet(),
                        largeImport = entries.size > 100,
                    )
                }
            } catch (_: FetchShikimoriImportEntries.NotLoggedInException) {
                mutableState.update { State.Error(mediaType, ErrorKind.NOT_LOGGED_IN) }
            } catch (_: Exception) {
                mutableState.update { State.Error(mediaType, ErrorKind.EMPTY) }
            }
        }
    }

    private fun catalogueSources(mediaType: ShikimoriImportMediaType): List<SourceChoice> = when (mediaType) {
        ShikimoriImportMediaType.ANIME -> animeSourceManager.getCatalogueSources().map { source ->
            SourceChoice(
                id = source.id,
                name = source.name,
                selected = false,
                recommendation = AnixartSourceHints.recommendation(source.name),
            )
        }
        ShikimoriImportMediaType.MANGA -> mangaSourceManager.getCatalogueSources().map { source ->
            SourceChoice(
                id = source.id,
                name = source.name,
                selected = false,
                recommendation = AnixartSourceHints.Recommendation.NEUTRAL,
            )
        }
        ShikimoriImportMediaType.RANOBE -> novelSourceManager.getCatalogueSources().map { source ->
            SourceChoice(
                id = source.id,
                name = source.name,
                selected = false,
                recommendation = AnixartSourceHints.Recommendation.NEUTRAL,
            )
        }
    }

    private suspend fun loadCategories(mediaType: ShikimoriImportMediaType): List<CategoryUi> = when (mediaType) {
        ShikimoriImportMediaType.ANIME -> getAnimeCategories.await().map { CategoryUi(it.id, it.name) }
        ShikimoriImportMediaType.MANGA -> getMangaCategories.await().map { CategoryUi(it.id, it.name) }
        ShikimoriImportMediaType.RANOBE -> getNovelCategories.await().map { CategoryUi(it.id, it.name) }
    }

    fun toggleSource(id: Long) {
        mutableState.update { s ->
            if (s !is State.PickSources) return@update s
            s.copy(sources = s.sources.map { if (it.id == id) it.copy(selected = !it.selected) else it })
        }
    }

    fun toggleStatusFilter(status: ShikimoriImportStatus) {
        mutableState.update { s ->
            if (s !is State.PickSources) return@update s
            val allowed = ShikimoriImportStatus.forMediaType(s.mediaType)
            if (status !in allowed) return@update s
            val updated = s.statusFilter.toMutableSet()
            if (!updated.remove(status)) updated.add(status)
            if (updated.isEmpty()) updated.addAll(allowed)
            s.copy(statusFilter = updated)
        }
    }

    fun setCategoryMapping(status: ShikimoriImportStatus, categoryId: Long?) {
        mutableState.update { s ->
            if (s !is State.PickSources) return@update s
            val updated = s.statusCategoryIds.toMutableMap()
            if (categoryId == null) updated.remove(status) else updated[status] = categoryId
            s.copy(statusCategoryIds = updated)
        }
    }

    fun setFavoriteCategoryMapping(categoryId: Long?) {
        mutableState.update { s ->
            if (s !is State.PickSources) return@update s
            s.copy(favoriteCategoryId = categoryId)
        }
    }

    private fun filteredEntries(pick: State.PickSources): List<ShikimoriImportEntry> {
        return pick.entries.filter { entry ->
            val status = ShikimoriImportStatus.fromApi(entry.status)
            status == null || status in pick.statusFilter
        }
    }

    fun startMatching() {
        val current = state.value as? State.PickSources ?: return
        val sourceIds = current.sources.filter { it.selected }.map { it.id }
        if (sourceIds.isEmpty()) return
        val entries = filteredEntries(current)
        if (entries.isEmpty()) return

        val statusCategoryIds = current.statusCategoryIds
        val favoriteCategoryId = current.favoriteCategoryId
        val total = entries.size
        val sourceNames = current.sources.associate { it.id to it.name }
        val mediaType = current.mediaType

        mutableState.update { State.Matching(mediaType, 0, total) }
        screenModelScope.launch {
            val searcher = createSearcher(mediaType, sourceIds)
            val searchCache = ConcurrentHashMap<String, List<AnixartMatcher.SearchCandidate>>()
            val semaphore = Semaphore(2)
            val matchedCount = AtomicInteger(0)

            suspend fun cachedSearch(query: String) = searchCache.getOrPut(query) { searcher.search(query) }

            val items = entries.map { entry ->
                async {
                    semaphore.withPermit {
                        val rowMatch = AnixartMatchingCoordinator.matchTitles(
                            candidateTitles = entry.candidateTitles(),
                            searchQueries = entry.searchQueries(),
                            search = { cachedSearch(it) },
                        )
                        val currentMatched = matchedCount.incrementAndGet()
                        mutableState.update { State.Matching(mediaType, currentMatched, total) }
                        val sourceName = rowMatch.result.best?.candidate?.sourceId?.let { sourceNames[it] }
                        ReviewItem(
                            entry = entry,
                            result = rowMatch.result,
                            selectedId = rowMatch.result.best?.candidate?.id,
                            enabled = rowMatch.result.confidence != AnixartMatcher.Confidence.NO_MATCH,
                            matchedQuery = rowMatch.matchedQuery,
                            matchedSourceName = sourceName,
                        )
                    }
                }
            }.awaitAll()

            val matchingReport = AnixartMatchingCoordinator.summarize(
                items.map { AnixartMatchingCoordinator.RowMatch(it.result, it.matchedQuery) },
            )
            mutableState.update {
                State.Review(mediaType, items, matchingReport, statusCategoryIds, favoriteCategoryId)
            }
        }
    }

    private fun createSearcher(
        mediaType: ShikimoriImportMediaType,
        sourceIds: List<Long>,
    ): AnixartTitleSearcher = when (mediaType) {
        ShikimoriImportMediaType.ANIME -> AnixartSourceSearcher(animeSourceManager, sourceIds)
        ShikimoriImportMediaType.MANGA -> ShikimoriMangaSourceSearcher(mangaSourceManager, sourceIds)
        ShikimoriImportMediaType.RANOBE -> ShikimoriNovelSourceSearcher(novelSourceManager, sourceIds)
    }

    fun setSelection(rowIndex: Int, candidateId: Long?) {
        mutableState.update { s ->
            if (s !is State.Review) return@update s
            s.copy(items = s.items.mapIndexed { i, it -> if (i == rowIndex) it.copy(selectedId = candidateId) else it })
        }
    }

    fun setEnabled(rowIndex: Int, enabled: Boolean) {
        mutableState.update { s ->
            if (s !is State.Review) return@update s
            s.copy(items = s.items.mapIndexed { i, it -> if (i == rowIndex) it.copy(enabled = enabled) else it })
        }
    }

    fun selectedCount(): Int =
        (state.value as? State.Review)?.items?.count { it.enabled && it.selectedId != null } ?: 0

    fun startImport() {
        val review = state.value as? State.Review ?: return
        val actions = review.items.mapNotNull { item ->
            if (!item.enabled || item.selectedId == null) return@mapNotNull null
            val candidate = item.result.ranked.firstOrNull { it.candidate.id == item.selectedId }?.candidate
                ?: return@mapNotNull null
            val status = ShikimoriImportStatus.fromApi(item.entry.status)
            val cats = buildSet {
                status?.let { s -> review.statusCategoryIds[s]?.let(::add) }
            }
            Triple(item.entry, candidate, cats)
        }

        mutableState.update { State.Importing(review.mediaType, 0, actions.size) }
        screenModelScope.launch {
            val report = when (review.mediaType) {
                ShikimoriImportMediaType.ANIME -> {
                    val animeActions = actions.map { (entry, candidate, cats) ->
                        ImportShikimoriEntries.Action(entry, candidate, cats)
                    }
                    val raw = importAnimeEntries.await(animeActions) { current, total ->
                        mutableState.update { State.Importing(review.mediaType, current, total) }
                    }
                    ImportReport(raw.added, raw.alreadyInLibrary, raw.failed, raw.trackerBound)
                }
                ShikimoriImportMediaType.MANGA -> {
                    val mangaActions = actions.map { (entry, candidate, cats) ->
                        ImportShikimoriMangaEntries.Action(entry, candidate, cats)
                    }
                    val raw = importMangaEntries.await(mangaActions) { current, total ->
                        mutableState.update { State.Importing(review.mediaType, current, total) }
                    }
                    ImportReport(raw.added, raw.alreadyInLibrary, raw.failed, raw.trackerBound)
                }
                ShikimoriImportMediaType.RANOBE -> {
                    val novelActions = actions.map { (entry, candidate, cats) ->
                        ImportShikimoriNovelEntries.Action(entry, candidate, cats)
                    }
                    val raw = importNovelEntries.await(novelActions) { current, total ->
                        mutableState.update { State.Importing(review.mediaType, current, total) }
                    }
                    ImportReport(raw.added, raw.alreadyInLibrary, raw.failed, raw.trackerBound)
                }
            }
            mutableState.update { State.Done(review.mediaType, report, review.matchingReport) }
        }
    }
}
