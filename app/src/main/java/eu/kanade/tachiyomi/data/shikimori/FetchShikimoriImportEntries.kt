package eu.kanade.tachiyomi.data.shikimori

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi
import tachiyomi.data.shikimori.ShikimoriImportEntry
import tachiyomi.data.shikimori.ShikimoriImportMediaType

class FetchShikimoriImportEntries(
    private val trackerManager: TrackerManager,
) {

    class NotLoggedInException : Exception()

    suspend fun await(mediaType: ShikimoriImportMediaType): List<ShikimoriImportEntry> {
        val shikimori = trackerManager.shikimori
        if (!shikimori.isLoggedIn) throw NotLoggedInException()

        val userId = shikimori.api.getCurrentUser()
        return when (mediaType) {
            ShikimoriImportMediaType.ANIME -> fetchAnime(shikimori.api, userId)
            ShikimoriImportMediaType.MANGA -> fetchManga(shikimori.api, userId, ranobeOnly = false)
            ShikimoriImportMediaType.RANOBE -> fetchManga(shikimori.api, userId, ranobeOnly = true)
        }
    }

    private suspend fun fetchAnime(api: ShikimoriApi, userId: Int): List<ShikimoriImportEntry> {
        val rates = api.getAllUserAnimeRates(userId)
        val animeById = rates
            .map { it.targetId }
            .distinct()
            .chunked(50)
            .flatMap { chunk -> api.getAnimesByIds(chunk) }
            .associateBy { it.id }

        return rates.mapNotNull { rate ->
            val anime = animeById[rate.targetId] ?: return@mapNotNull null
            ShikimoriImportEntry(
                mediaType = ShikimoriImportMediaType.ANIME,
                rateId = rate.id,
                remoteId = rate.targetId,
                name = anime.name,
                russian = anime.russian,
                status = rate.status,
                score = rate.score,
                progress = rate.episodes,
                totalCount = anime.episodes,
                thumbnailUrl = ShikimoriApi.BASE_URL + anime.image.original,
            )
        }
    }

    private suspend fun fetchManga(
        api: ShikimoriApi,
        userId: Int,
        ranobeOnly: Boolean,
    ): List<ShikimoriImportEntry> {
        val rates = api.getAllUserMangaRates(userId)
        val targetIds = rates.map { it.targetId }.distinct()
        val mangaById = targetIds
            .chunked(50)
            .flatMap { chunk -> api.getMangasByIds(chunk) }
            .associateBy { it.id }
            .toMutableMap()

        // Bulk /mangas?ids= omits light_novel and novel entries; fetch them individually.
        for (id in targetIds) {
            if (id in mangaById) continue
            runCatching { api.getMangaById(id) }
                .onSuccess { mangaById[it.id] = it }
        }

        return rates.mapNotNull { rate ->
            val manga = mangaById[rate.targetId] ?: return@mapNotNull null
            val isRanobe = ShikimoriImportEntry.isRanobeKind(manga.kind)
            if (ranobeOnly != isRanobe) return@mapNotNull null
            ShikimoriImportEntry(
                mediaType = if (isRanobe) ShikimoriImportMediaType.RANOBE else ShikimoriImportMediaType.MANGA,
                rateId = rate.id,
                remoteId = rate.targetId,
                name = manga.name,
                russian = manga.russian,
                status = rate.status,
                score = rate.score,
                progress = rate.chapters,
                totalCount = manga.chapters,
                thumbnailUrl = ShikimoriApi.BASE_URL + manga.image.original,
                kind = manga.kind,
            )
        }
    }
}
