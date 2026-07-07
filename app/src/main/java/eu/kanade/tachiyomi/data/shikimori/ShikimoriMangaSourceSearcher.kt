package eu.kanade.tachiyomi.data.shikimori

import eu.kanade.tachiyomi.data.anixart.AnixartSourceRateLimiter
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.withTimeout
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.anixart.AnixartMatcher
import tachiyomi.data.anixart.AnixartMatchingCoordinator
import tachiyomi.data.anixart.AnixartTitleSearcher
import tachiyomi.domain.source.manga.service.MangaSourceManager

class ShikimoriMangaSourceSearcher(
    private val sourceManager: MangaSourceManager,
    private val sourceIds: List<Long>,
    private val rateLimiter: AnixartSourceRateLimiter = AnixartSourceRateLimiter(),
    private val maxResultsPerSource: Int = MAX_RESULTS_PER_SOURCE,
    private val sourceTimeoutMs: Long = SOURCE_TIMEOUT_MS,
) : AnixartTitleSearcher {

    override suspend fun search(query: String): List<AnixartMatcher.SearchCandidate> {
        if (query.isBlank()) return emptyList()
        val results = ArrayList<AnixartMatcher.SearchCandidate>()
        for (sourceId in sourceIds) {
            val source = sourceManager.get(sourceId) as? CatalogueSource ?: continue
            val page = try {
                rateLimiter.withRateLimit(sourceId) {
                    withTimeout(sourceTimeoutMs) {
                        source.getSearchManga(1, query, FilterList())
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Shikimori manga search failed on source $sourceId for '$query'" }
                continue
            }
            for (sManga in page.mangas.take(maxResultsPerSource)) {
                results += AnixartMatcher.SearchCandidate(
                    id = (sourceId.toString() + sManga.url).hashCode().toLong(),
                    sourceId = sourceId,
                    displayTitle = sManga.title,
                    titles = listOf(sManga.title).filter { it.isNotBlank() },
                    url = sManga.url,
                    thumbnailUrl = sManga.thumbnail_url,
                )
            }
        }
        return AnixartMatchingCoordinator.dedupCandidates(results)
    }

    companion object {
        const val MAX_RESULTS_PER_SOURCE = 10
        const val SOURCE_TIMEOUT_MS = 8_000L
    }
}
