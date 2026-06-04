package dev.patrickgold.florisboard.app.settings.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

@Composable
fun AiSettingsScreen() = FlorisScreen {
    title = "ИИ Функции"
    previewFieldVisible = true

    content {
        val prefs by FlorisPreferenceStore
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Настройки API",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AiPromptEditor(
                pref = prefs.ai.apiKey,
                title = "Gemini API Key",
                summary = "Введите ваш API-ключ Gemini (начинается с AIzaSy). Без ключа функции ИИ работать не будут."
            )
            Text(
                text = "Инструкции для ИИ стилей",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            AiPromptEditor(
                pref = prefs.ai.formalPrompt, 
                title = "Формальный стиль (👔)", 
                summary = "Инструкция, которая будет передана ИИ при выборе формального стиля."
            )
            AiPromptEditor(
                pref = prefs.ai.friendlyPrompt, 
                title = "Дружелюбный стиль (😊)", 
                summary = "Инструкция для дружелюбного и неформального стиля."
            )
            AiPromptEditor(
                pref = prefs.ai.grammarPrompt, 
                title = "Исправление ошибок (📝)", 
                summary = "Инструкция для проверки и исправления грамматики."
            )
            AiPromptEditor(
                pref = prefs.ai.funnyPrompt, 
                title = "Саркастичный/зумерский стиль (🃏)", 
                summary = "Инструкция для шуточного молодежного стиля."
            )
            AiPromptEditor(
                pref = prefs.ai.screenshotPrompt, 
                title = "Оцифровка фото (📸)", 
                summary = "Инструкция для извлечения текста из изображений."
            )
        }
    }
}

@Composable
private fun AiPromptEditor(
    pref: PreferenceData<String>,
    title: String,
    summary: String
) {
    val value by pref.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = summary, 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { newValue -> 
                scope.launch {
                    pref.set(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 10
        )
    }
}
