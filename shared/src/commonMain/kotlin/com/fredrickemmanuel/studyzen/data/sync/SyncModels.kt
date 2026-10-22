package com.fredrickemmanuel.studyzen.data.sync

import kotlinx.serialization.Serializable

/** One item (a Study, a session, a plan...) as it travels to and from the server. */
@Serializable
data class SyncChange(
    val collection: String,
    val id: String,
    /** The item as JSON text; null for a deletion. */
    val data: String? = null,
    /** When the learner last changed it, by this phone's clock. The newest change wins. */
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
data class SyncRequest(val cursor: Long, val changes: List<SyncChange>)

@Serializable
data class SyncResponse(val cursor: Long, val changes: List<SyncChange> = emptyList(), val hasMore: Boolean = false)

/** Sends changes to the server and brings back the ones this phone has not seen. */
interface SyncTransport {
    suspend fun exchange(request: SyncRequest): SyncResponse
}

/** What this phone remembers about one item: how it looked at the last sync, and whether it has unsent edits. */
@Serializable
data class SyncMeta(
    val updatedAt: Long,
    val hash: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
)

@Serializable
data class SyncState(
    /** The account whose data is on this phone. Another account logging in starts clean. */
    val owner: String? = null,
    /** The number of the last server change this phone has seen. */
    val cursor: Long = 0,
    val lastFullPullAt: Long = 0,
    val items: Map<String, SyncMeta> = emptyMap(),
)

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val atMillis: Long) : SyncStatus
    data class Failed(val message: String, val offline: Boolean) : SyncStatus
}
