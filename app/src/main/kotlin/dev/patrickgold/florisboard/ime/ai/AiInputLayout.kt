package dev.patrickgold.florisboard.ime.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.keyboard.AiGenerationState
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import org.florisboard.lib.compose.florisVerticalScroll
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

@Composable
fun AiInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val aiUiState by keyboardManager.aiUiState.collectAsState()

    SnyggColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.imeUiHeight()),
    ) {
        // Header
        SnyggRow(
            elementName = FlorisImeUi.ClipboardHeader.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIconButton(
                elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                onClick = { keyboardManager.cancelAiGeneration() },
                modifier = Modifier
                    .sizeIn(maxHeight = FlorisImeSizing.smartbarHeight)
                    .aspectRatio(1f),
            ) {
                SnyggIcon(imageVector = Icons.AutoMirrored.Filled.ArrowBack)
            }
            SnyggText(
                elementName = FlorisImeUi.ClipboardHeaderText.elementName,
                modifier = Modifier.weight(1f),
                text = "Генерация ИИ",
            )
        }

        // Content
        SnyggBox(
            elementName = FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (aiUiState.state) {
                AiGenerationState.IDLE, AiGenerationState.GENERATING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    }
                }
                AiGenerationState.ERROR -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SnyggText(text = "Ошибка генерации:")
                        SnyggText(text = aiUiState.error ?: "Неизвестная ошибка")
                        Spacer(modifier = Modifier.height(16.dp))
                        SnyggButton(
                            elementName = FlorisImeUi.ClipboardClearAllDialogButton.elementName,
                            onClick = { keyboardManager.cancelAiGeneration() }
                        ) {
                            SnyggText(text = "Назад")
                        }
                    }
                }
                AiGenerationState.DONE -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .florisVerticalScroll()
                        ) {
                            SnyggText(
                                modifier = Modifier.padding(8.dp),
                                text = aiUiState.generatedText ?: ""
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SnyggButton(
                                elementName = FlorisImeUi.ClipboardClearAllDialogButton.elementName,
                                onClick = { 
                                    if (aiUiState.currentStyle != null && aiUiState.currentSourceText != null) {
                                        keyboardManager.executeAiGeneration(
                                            aiUiState.currentStyle!!,
                                            aiUiState.currentSourceText!!
                                        )
                                    }
                                }
                            ) {
                                SnyggText(text = "Попробовать ещё")
                            }
                            
                            SnyggButton(
                                elementName = FlorisImeUi.ClipboardClearAllDialogButton.elementName,
                                onClick = { keyboardManager.commitAiText() }
                            ) {
                                SnyggText(text = "Вставить текст")
                            }
                        }
                    }
                }
            }
        }
    }
}
