package eu.kanade.tachiyomi.data.shikimori

import eu.kanade.tachiyomi.data.anixart.AnixartSourceRateLimiter
import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.novelsource.model.NovelFilterList
import kotlinx.coroutines.withTimeout
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.anixart.AnixartMatcher
import tachiyomi.data.anixart.AnixartMatchingCoordinator
import tachiyomi.data.anixart.AnixartTitleSearcher
import tachiyomi.domain.source.novel.service.NovelSourceManager

class ShikimoriNovelSourceSearcher(
    private val sourceManager: NovelSourceManager,
    private val sourceIds: List<Long>,
    private val rateLimiter: AnixartSourceRateLimiter = AnixartSourceRateLimiter(),
    private val maxResultsPerSource: Int = MAX_RESULTS_PER_SOURCE,
    private val sourceTimeoutMs: Long = SOURCE_TIMEOUT_MS,
) : AnixartTitleSearcher {

    override suspend fun search(query: String): List<AnixartMatcher.SearchCandidate> {
        if (query.isBlank()) return emptyList()
        val results = ArrayList<AnixartMatcher.SearchCandidate>()
        for (sourceId in sourceIds) {
            val source = sourceManager.get(sourceId) as? NovelCatalogueSource ?: continue
            val page = try {
                rateLimiter.withRateLimit(sourceId) {
                    withTimeout(sourceTimeoutMs) {
                        source.getSearchNovels(1, query, NovelFilterList())
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Shikimori ranobe search failed on source $sourceId for '$query'" }
                continue
            }
            for (sNovel in page.novels.take(maxResultsPerSource)) {
                results += AnixartMatcher.SearchCandidate(
                    id = (sourceId.toString() + sNovel.url).hashCode().toLong(),
                    sourceId = sourceId,
                    displayTitle = sNovel.title,
                    titles = listOf(sNovel.title).filter { it.isNotBlank() },
                    url = sNovel.url,
                    thumbnailUrl = sNovel.thumbnail_url,
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
