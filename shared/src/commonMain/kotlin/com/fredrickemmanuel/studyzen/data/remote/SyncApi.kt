package com.fredrickemmanuel.studyzen.data.remote

import com.fredrickemmanuel.studyzen.data.sync.SyncRequest
import com.fredrickemmanuel.studyzen.data.sync.SyncResponse
import com.fredrickemmanuel.studyzen.data.sync.SyncTransport
import kotlinx.serialization.serializer

/** Talks to `POST /sync` on the Node server. */
class SyncApi(private val client: AuthedClient = AuthedClient()) : SyncTransport {
    override suspend fun exchange(request: SyncRequest): SyncResponse {
        val text = client.post("/sync", ApiJson.encodeToString(serializer<SyncRequest>(), request))
        return ApiJson.decodeFromString(serializer<SyncResponse>(), text)
    }
}
