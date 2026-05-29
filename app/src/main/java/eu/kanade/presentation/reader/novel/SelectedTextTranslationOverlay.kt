package eu.kanade.presentation.reader.novel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.novel.NovelReaderScreenModel
import eu.kanade.tachiyomi.ui.reader.novel.NovelSelectedTextTranslationErrorReason
import eu.kanade.tachiyomi.ui.reader.novel.NovelSelectedTextTranslationUiState
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SelectedTextTranslationOverlay(
    state: NovelReaderScreenModel.State.Success,
    onTranslate: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.readerSettings.selectedTextTranslationEnabled) return

    val selection = state.selectedTextTranslationSelection
    val translationState = state.selectedTextTranslationUiState

    if (selection == null && translationState is NovelSelectedTextTranslationUiState.Idle) {
        return
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .widthIn(max = 360.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (translationState) {
            is NovelSelectedTextTranslationUiState.SelectionAvailable -> {
                FloatingActionButton(
                    onClick = onTranslate,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = stringResource(
                            AYMR.strings.novel_reader_selected_text_translation_action_translate,
                        ),
                    )
                }
            }
            is NovelSelectedTextTranslationUiState.Translating -> {
                SelectedTextTranslationCard(
                    title = stringResource(AYMR.strings.novel_reader_selected_text_translation_loading),
                    subtitle = selection?.text,
                    onDismiss = onDismiss,
                    trailingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    },
                )
            }
            is NovelSelectedTextTranslationUiState.Result -> {
                SelectedTextTranslationCard(
                    title = selection?.text,
                    subtitle = translationState.translationResult.translation,
                    onDismiss = onDismiss,
                )
            }
            is NovelSelectedTextTranslationUiState.Error -> {
                SelectedTextTranslationCard(
                    title = selection?.text,
                    subtitle = translationErrorMessage(translationState.reason),
                    onDismiss = onDismiss,
                    actionLabel = stringResource(
                        AYMR.strings.novel_reader_selected_text_translation_action_retry,
                    ),
                    onAction = onRetry,
                )
            }
            is NovelSelectedTextTranslationUiState.Unavailable -> {
                SelectedTextTranslationCard(
                    title = selection?.text,
                    subtitle = translationErrorMessage(translationState.reason),
                    onDismiss = onDismiss,
                    actionLabel = stringResource(
                        AYMR.strings.novel_reader_selected_text_translation_action_retry,
                    ),
                    onAction = onRetry,
                )
            }
            NovelSelectedTextTranslationUiState.Idle -> {
                if (selection != null) {
                    FloatingActionButton(
                        onClick = onTranslate,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = stringResource(
                                AYMR.strings.novel_reader_selected_text_translation_action_translate,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedTextTranslationCard(
    title: String?,
    subtitle: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 328.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title?.takeIf { it.isNotBlank() }
                        ?: stringResource(AYMR.strings.novel_reader_selected_text_translation_action_translate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trailingContent != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    trailingContent()
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(AYMR.strings.novel_reader_selected_text_translation_action_close))
                }
            }
        }
    }
}

@Composable
private fun translationErrorMessage(reason: NovelSelectedTextTranslationErrorReason): String {
    return when (reason) {
        NovelSelectedTextTranslationErrorReason.EmptySelection,
        NovelSelectedTextTranslationErrorReason.TooLongSelection,
        NovelSelectedTextTranslationErrorReason.ParserFailure,
        NovelSelectedTextTranslationErrorReason.WebViewUnavailable,
        -> {
            stringResource(AYMR.strings.novel_reader_selected_text_translation_unavailable)
        }
        is NovelSelectedTextTranslationErrorReason.BackendUnavailable -> {
            reason.message?.takeIf { it.isNotBlank() }
                ?: stringResource(AYMR.strings.novel_reader_selected_text_translation_unavailable)
        }
        is NovelSelectedTextTranslationErrorReason.NetworkFailure -> {
            reason.message?.takeIf { it.isNotBlank() }
                ?: stringResource(AYMR.strings.novel_reader_selected_text_translation_unavailable)
        }
        is NovelSelectedTextTranslationErrorReason.Cooldown -> {
            "${stringResource(
                AYMR.strings.novel_reader_selected_text_translation_unavailable,
            )} (${reason.remainingSeconds}s)"
        }
    }
}
