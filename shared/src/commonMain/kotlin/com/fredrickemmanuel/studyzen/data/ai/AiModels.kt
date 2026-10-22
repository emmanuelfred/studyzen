package com.fredrickemmanuel.studyzen.data.ai

import kotlinx.serialization.Serializable

/**
 * The AI services a learner can connect with their own free key. StudyZen is open source and
 * cannot pay for everyone's AI, so each learner brings a key; nothing is sent through our server.
 */
@Serializable
enum class AiProvider(
    val label: String,
    /** Used when the learner has not chosen another model. Models change often: it can be edited. */
    val defaultModel: String,
    val keyUrl: String,
    val tagline: String,
) {
    Gemini(
        label = "Google Gemini",
        defaultModel = "gemini-3-flash-preview",
        keyUrl = "https://aistudio.google.com/apikey",
        tagline = "Free key with a Google account. Reads photos well, and is the best place to start.",
    ),
    Groq(
        label = "Groq",
        defaultModel = "qwen/qwen3.8-27b",
        keyUrl = "https://console.groq.com/keys",
        tagline = "Free key, very fast answers. Free plan has daily limits.",
    ),
    OpenRouter(
        label = "OpenRouter",
        defaultModel = "openrouter/free",
        keyUrl = "https://openrouter.ai/keys",
        tagline = "One free key that picks any free model available. Can be slower or busy.",
    ),
}

@Serializable
data class AiProviderConfig(
    val provider: AiProvider,
    val apiKey: String,
    /** Blank means "use the default for this provider". */
    val model: String = "",
) {
    val effectiveModel: String get() = model.trim().ifEmpty { provider.defaultModel }
}

/** The keys a learner has added, and which one StudyZen should try first. */
@Serializable
data class AiSettings(
    val configs: List<AiProviderConfig> = emptyList(),
    val primary: AiProvider? = null,
) {
    fun configFor(provider: AiProvider): AiProviderConfig? = configs.firstOrNull { it.provider == provider }

    /** Providers that have a key, the main one first. */
    fun ready(): List<AiProviderConfig> =
        configs.filter { it.apiKey.isNotBlank() }.sortedBy { if (it.provider == primary) 0 else 1 }

    val hasKey: Boolean get() = configs.any { it.apiKey.isNotBlank() }
}

enum class AiRole { User, Assistant }

/** A photo sent to the AI, already saved as base64 text. */
data class AiImage(val mimeType: String, val base64: String)

data class AiMessage(val role: AiRole, val text: String, val image: AiImage? = null)

enum class AiFailure { NoKey, BadKey, RateLimited, ModelProblem, TooLarge, Offline, Server, Empty, Other }

sealed interface AiResult {
    data class Success(val text: String, val provider: AiProvider, val model: String) : AiResult
    data class Failure(val kind: AiFailure, val message: String, val provider: AiProvider? = null) : AiResult
}
