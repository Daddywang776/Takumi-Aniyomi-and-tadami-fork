package eu.kanade.tachiyomi.data.download.engine

import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.data.download.novel.NovelDownloadQueueManager
import eu.kanade.tachiyomi.data.download.novel.NovelDownloadQueueState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/**
 * Combines anime, manga, and novel backend queue states into one aggregated
 * [DownloadEngineSnapshot] and routes global pause/resume/cancel actions.
 */
class DownloadEngineFacade(
    private val animeManager: AnimeDownloadManager,
    private val mangaManager: MangaDownloadManager,
    private val speedTracker: DownloadSpeedTracker = DownloadSpeedTracker(),
    private val completionTracker: DownloadCompletionTracker = DownloadCompletionTracker(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val telemetryCollector = DownloadTelemetryCollector(speedTracker)
    private val storageManager: StorageManager = Injekt.get()

    private val _state = MutableStateFlow(DownloadEngineSnapshot())
    val state: StateFlow<DownloadEngineSnapshot> = _state.asStateFlow()

    init {
        animeManager.telemetryEmitter = telemetryCollector
        animeManager.completionTracker = completionTracker
        mangaManager.telemetryEmitter = telemetryCollector
        mangaManager.completionTracker = completionTracker
        NovelDownloadQueueManager.telemetryEmitter = telemetryCollector
        NovelDownloadQueueManager.completionTracker = completionTracker

        val animeQueueFlow = animeManager.queueState.transformLatest { queue ->
            if (queue.isEmpty()) {
                emit(queue)
            } else {
                combine(queue.map { it.statusFlow }) { _ -> queue }.collect { emit(it) }
            }
        }

        val mangaQueueFlow = mangaManager.queueState.transformLatest { queue ->
            if (queue.isEmpty()) {
                emit(queue)
            } else {
                combine(queue.map { it.statusFlow }) { _ -> queue }.collect { emit(it) }
            }
        }

        scope.launch {
            combine(
                animeQueueFlow,
                mangaQueueFlow,
                NovelDownloadQueueManager.state,
                animeManager.isDownloaderRunning,
                mangaManager.isDownloaderRunning,
                telemetryCollector.version,
            ) { array ->
                val animeQueue = array[0] as List<AnimeDownload>
                val mangaQueue = array[1] as List<MangaDownload>
                val novelState = array[2] as NovelDownloadQueueState
                buildSnapshot(animeQueue, mangaQueue, novelState)
            }.collect { snapshot -> _state.update { snapshot } }
        }
    }

    private fun buildSnapshot(
        animeQueue: List<AnimeDownload>,
        mangaQueue: List<MangaDownload>,
        novelState: NovelDownloadQueueState,
    ): DownloadEngineSnapshot {
        val animeActive = animeQueue.count { it.status == AnimeDownload.State.DOWNLOADING }
        val animeQueued = animeQueue.count { it.status == AnimeDownload.State.QUEUE }
        val animeFailed = animeQueue.count { it.status == AnimeDownload.State.ERROR }

        val mangaActive = mangaQueue.count { it.status == MangaDownload.State.DOWNLOADING }
        val mangaQueued = mangaQueue.count { it.status == MangaDownload.State.QUEUE }
        val mangaFailed = mangaQueue.count { it.status == MangaDownload.State.ERROR }
        val speedSnapshot = speedTracker.snapshot()
        val downloadsDir = storageManager.getDownloadsDirectory()?.filePath
        val freeSpaceBytes = downloadsDir?.let { File(it).freeSpace } ?: 0L

        return DownloadEngineSnapshot(
            animeItems = animeQueue.size,
            animeActive = animeActive,
            animeQueued = animeQueued,
            animeFailed = animeFailed,
            mangaItems = mangaQueue.size,
            mangaActive = mangaActive,
            mangaQueued = mangaQueued,
            mangaFailed = mangaFailed,
            novelPending = novelState.pendingCount,
            novelActive = novelState.activeCount,
            novelFailed = novelState.failedCount,
            animeRunning = animeManager.isRunning,
            mangaRunning = mangaManager.isRunning,
            novelRunning = novelState.isRunning,
            sessionCompleted = completionTracker.totalCompletions,
            currentSpeedBps = speedSnapshot.currentSpeedBps,
            averageSpeedBps = speedSnapshot.averageSpeedBps,
            etaMillis = speedSnapshot.etaMillis,
            speedHistoryBps = speedSnapshot.speedHistoryBps,
            animeStorageBytes = animeManager.getDownloadSize(),
            mangaStorageBytes = mangaManager.getDownloadSize(),
            novelStorageBytes = NovelDownloadQueueManager.getDownloadSize(),
            freeSpaceBytes = freeSpaceBytes,
        )
    }

    fun pauseAll() {
        animeManager.pauseDownloads()
        mangaManager.pauseDownloads()
        NovelDownloadQueueManager.pauseDownloads()
    }

    fun resumeAll() {
        animeManager.startDownloads()
        mangaManager.startDownloads()
        NovelDownloadQueueManager.startDownloads()
    }

    fun cancelAll() {
        animeManager.clearQueue()
        mangaManager.clearQueue()
        NovelDownloadQueueManager.clearQueue()
    }
}
