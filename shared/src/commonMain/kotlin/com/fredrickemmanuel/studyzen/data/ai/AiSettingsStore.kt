package com.fredrickemmanuel.studyzen.data.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.fredrickemmanuel.studyzen.data.KeyValueStore
import com.fredrickemmanuel.studyzen.data.platformKeyValueStore
import kotlinx.serialization.json.Json

/**
 * Keeps the learner's AI keys on this device only (they are never sent to the StudyZen server and
 * never synced). Stored in app-private storage; hardware-backed encryption is a planned hardening step.
 */
class AiSettingsStore(private val store: KeyValueStore = platformKeyValueStore()) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var settings: AiSettings by mutableStateOf(load())
        private set

    private fun load(): AiSettings {
        val raw = store.get(KEY) ?: return AiSettings()
        return try {
            json.decodeFromString(AiSettings.serializer(), raw)
        } catch (e: Exception) {
            AiSettings()
        }
    }

    private fun commit(next: AiSettings) {
        settings = next
        store.put(KEY, json.encodeToString(AiSettings.serializer(), next))
    }

    /** Saves (or changes) the key for one provider. The first key added becomes the main one. */
    fun save(provider: AiProvider, apiKey: String, model: String) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            remove(provider)
            return
        }
        val config = AiProviderConfig(provider, key, model.trim())
        val others = settings.configs.filter { it.provider != provider }
        val primary = settings.primary?.takeIf { p -> p == provider || others.any { it.provider == p } } ?: provider
        commit(AiSettings(others + config, primary))
    }

    fun remove(provider: AiProvider) {
        val rest = settings.configs.filter { it.provider != provider }
        val primary = settings.primary?.takeIf { p -> rest.any { it.provider == p } } ?: rest.firstOrNull()?.provider
        commit(AiSettings(rest, primary))
    }

    fun makePrimary(provider: AiProvider) {
        if (settings.configFor(provider) != null) commit(settings.copy(primary = provider))
    }

    private companion object {
        const val KEY = "ai.settings.v1"
    }
}

val LocalAiSettings = staticCompositionLocalOf<AiSettingsStore> { error("AiSettingsStore not provided") }
