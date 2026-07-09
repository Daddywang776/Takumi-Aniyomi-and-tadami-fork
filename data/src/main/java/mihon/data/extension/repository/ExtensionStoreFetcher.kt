package mihon.data.extension.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import mihon.data.extension.model.AvailableExtensionData
import mihon.data.extension.service.ExtensionStoreService
import mihon.domain.extensionstore.model.ExtensionStore
import tachiyomi.core.common.util.system.logcat

class ExtensionStoreFetcher(
    private val service: ExtensionStoreService,
) {
    suspend fun fetchExtensions(stores: List<ExtensionStore>): List<AvailableExtensionData> {
        if (stores.isEmpty()) return emptyList()
        return supervisorScope {
            stores.map { store ->
                async {
                    service.getExtensions(store).onFailure {
                        logcat(LogPriority.ERROR, it) {
                            "Failed to fetch extensions for store '${store.name} (${store.indexUrl})'"
                        }
                    }
                }
            }
                .awaitAll()
                .flatMap { it.getOrDefault(emptyList()) }
        }
    }
}
