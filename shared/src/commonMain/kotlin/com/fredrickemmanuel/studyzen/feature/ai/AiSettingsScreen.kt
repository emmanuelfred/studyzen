package com.fredrickemmanuel.studyzen.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.data.ai.AiMessage
import com.fredrickemmanuel.studyzen.data.ai.AiProvider
import com.fredrickemmanuel.studyzen.data.ai.AiProviderConfig
import com.fredrickemmanuel.studyzen.data.ai.AiResult
import com.fredrickemmanuel.studyzen.data.ai.AiRole
import com.fredrickemmanuel.studyzen.data.ai.AiService
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.StudyPrompts
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import kotlinx.coroutines.launch

/**
 * Where the learner adds their own AI keys. StudyZen is free and open source, so it cannot pay for
 * everyone's AI: each learner connects a free account. Several can be added; the main one is tried
 * first and the others take over if it runs out.
 */
@Composable
fun AiSettingsScreen(modifier: Modifier = Modifier) {
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val store = LocalAiSettings.current
    val service = remember { AiService() }

    ScreenScaffold(title = "AI settings", onBack = nav::back, modifier = modifier) {
        Spacer(Modifier.height(8.dp))
        InfoBanner(
            text = "StudyZen is free and open source, so it has no AI of its own. Add a free key from " +
                "one (or more) of these services and the scanner will use it. If one runs out for the day, " +
                "StudyZen switches to the next.",
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AiProvider.entries.forEach { provider ->
                ProviderCard(provider, store, service)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Your keys stay on this phone and are sent only to the service they belong to. They are not saved on " +
                "StudyZen's server or synced to your other phones. Free plans (Gemini's, for example) may use what you " +
                "send to improve their models, so avoid private information in photos and questions.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProviderCard(
    provider: AiProvider,
    store: com.fredrickemmanuel.studyzen.data.ai.AiSettingsStore,
    service: AiService,
) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val saved = store.settings.configFor(provider)
    val isMain = store.settings.primary == provider && saved != null

    var key by remember(saved?.apiKey) { mutableStateOf(saved?.apiKey.orEmpty()) }
    var model by remember(saved?.model) { mutableStateOf(saved?.model.orEmpty()) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<AiResult?>(null) }

    val changed = key.trim() != saved?.apiKey.orEmpty() || model.trim() != saved?.model.orEmpty()

    StudyZenCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(provider.label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
            when {
                isMain -> TagChip("Main")
                saved != null -> TagChip("Ready")
            }
        }
        Text(
            provider.tagline,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        StudyZenTextField(
            value = key,
            onValueChange = { key = it; testResult = null },
            label = "API key",
            placeholder = "Paste your key here",
            isPassword = true,
        )
        Spacer(Modifier.height(10.dp))
        StudyZenTextField(
            value = model,
            onValueChange = { model = it; testResult = null },
            label = "Model (optional)",
            placeholder = provider.defaultModel,
        )

        testResult?.let { result ->
            Spacer(Modifier.height(10.dp))
            when (result) {
                is AiResult.Success -> InfoBanner("It works. ${provider.label} answered using ${result.model}.")
                is AiResult.Failure -> InfoBanner(result.message, tone = BannerTone.Error)
            }
        }

        Spacer(Modifier.height(12.dp))
        StudyZenButton(
            "Save",
            onClick = {
                store.save(provider, key, model)
                testResult = null
            },
            enabled = changed && key.isNotBlank(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StudyZenTextButton("Get a free key", onClick = { uriHandler.openUri(provider.keyUrl) })
            StudyZenTextButton(
                if (testing) "Testing…" else "Test",
                enabled = key.isNotBlank() && !testing,
                onClick = {
                    testing = true
                    testResult = null
                    scope.launch {
                        testResult = service.askOne(
                            AiProviderConfig(provider, key.trim(), model.trim()),
                            StudyPrompts.TEST_SYSTEM,
                            listOf(AiMessage(AiRole.User, StudyPrompts.TEST_MESSAGE)),
                        )
                        testing = false
                    }
                },
            )
        }
        if (saved != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isMain) StudyZenTextButton("Make main", onClick = { store.makePrimary(provider) })
                StudyZenTextButton("Remove", onClick = {
                    store.remove(provider)
                    key = ""
                    model = ""
                    testResult = null
                })
            }
        }
    }
}
