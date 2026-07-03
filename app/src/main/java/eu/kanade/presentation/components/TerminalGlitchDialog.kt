package eu.kanade.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/* =============================================================================
 *  TERMINAL GLITCH DIALOG (v2) — на базе GlitchStack.
 *
 *  Отличия от v1:
 *   • CRT power-on при появлении (окно "включается" из линии);
 *   • скрембл-печать (rememberScrambleReveal) вместо ровного typewriter;
 *   • хроматический дрожащий заголовок (RGB-split);
 *   • скан-линии + статик поверх scrim;
 *   • растущие трещины внутри окна;
 *   • hazard-кнопки (диагональные полосы) + опция press-and-hold.
 *
 *  Сигнатура совместима с v1 — шаги 2 и 3 продолжают работать без изменений.
 * ========================================================================== */

@Composable
fun TerminalGlitchDialog(
    title: String,
    message: String,
    buttonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {},
    dismissButtonText: String? = null,
    holdToConfirm: Boolean = false,
    accent: Color = GlitchPalette.HazardRed,
) {
    val time by rememberGlitchTime()

    // Скрембл-печать тела сообщения
    val displayedText = rememberScrambleReveal(message, charDelayMs = 48, scramblePerChar = 2)

    // Мигающий курсор
    val infinite = rememberInfiniteTransition(label = "cursor")
    val cursorOn by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursor_blink",
    )

    // CRT power-on при первом кадре
    var powered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { powered = true }

    // Растущие трещины
    val crack = remember { Animatable(0f) }
    LaunchedEffect(Unit) { crack.animateTo(1f, tween(2600, easing = LinearEasing)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center,
        ) {
            // Скан-линии + статик поверх scrim
            ScanlineOverlay(intensity = 0.7f, time = time, modifier = Modifier.fillMaxSize())
            StaticNoiseOverlay(intensity = 0.5f, time = time, modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 360.dp)
                    .crtPowerOn(powered)
                    .clip(CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp))
                    .background(GlitchPalette.Void)
                    // пульсирующий алый бордер + свечение
                    .border(
                        width = 2.dp,
                        color = accent.copy(alpha = 0.65f + 0.35f * cursorOn),
                        shape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp),
                    )
                    .padding(20.dp),
            ) {
                // Растущие трещины на фоне окна
                Canvas(modifier = Modifier.matchParentSize()) {
                    val p = crack.value
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.2f)
                        lineTo(size.width * 0.15f * p, size.height * 0.28f)
                        lineTo(size.width * 0.10f * p, size.height * 0.45f)
                        moveTo(size.width, size.height * 0.75f)
                        lineTo(size.width * (1f - 0.15f * p), size.height * 0.65f)
                        lineTo(size.width * (1f - 0.10f * p), size.height * 0.50f)
                        lineTo(size.width * (1f - 0.22f * p), size.height * 0.45f)
                        moveTo(size.width * 0.5f, 0f)
                        lineTo(size.width * 0.55f, size.height * 0.18f * p)
                        lineTo(size.width * 0.47f, size.height * 0.30f * p)
                    }
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.30f * p),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Хроматический заголовок
                    GlitchTitle(text = title, accent = accent, time = time)

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 104.dp)
                            .background(Color(0xFF050001))
                            .border(1.dp, accent.copy(alpha = 0.35f))
                            .padding(12.dp),
                    ) {
                        val cursor = if (cursorOn > 0.5f) "\u2588" else " "
                        Text(
                            text = "$displayedText$cursor",
                            color = GlitchPalette.Phosphor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (dismissButtonText != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HazardButton(
                                text = dismissButtonText,
                                accent = accent,
                                filled = false,
                                modifier = Modifier.weight(1f),
                                onClick = onDismiss,
                            )
                            HazardButton(
                                text = buttonText,
                                accent = accent,
                                filled = true,
                                holdToConfirm = holdToConfirm,
                                modifier = Modifier.weight(1f),
                                onClick = onConfirm,
                            )
                        }
                    } else {
                        HazardButton(
                            text = buttonText,
                            accent = accent,
                            filled = true,
                            holdToConfirm = holdToConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onConfirm,
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
//  Хроматический заголовок (RGB-split через 3 смещённых слоя)
// -----------------------------------------------------------------------------
@Composable
private fun GlitchTitle(text: String, accent: Color, time: Float) {
    // лёгкое дрожание смещения (может быть отрицательным)
    val jitter = (kotlin.math.sin(time * 30f) * 1.5f).dp
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = accent.copy(alpha = 0.8f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().offset(x = jitter),
        )
        Text(
            text = text,
            color = Color(0xFF00E5FF).copy(alpha = 0.6f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().offset(x = -jitter),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// -----------------------------------------------------------------------------
//  Hazard-кнопка (диагональные полосы) + опция press-and-hold
// -----------------------------------------------------------------------------
@Composable
private fun HazardButton(
    text: String,
    accent: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    holdToConfirm: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val base = modifier
        .height(50.dp)
        .clip(shape)
        .background(if (filled) accent.copy(alpha = 0.14f) else Color(0xFF160306))
        .border(1.dp, accent.copy(alpha = if (filled) 0.9f else 0.5f), shape)
        // диагональные hazard-полосы для залитой кнопки
        .then(
            if (filled) {
                Modifier.drawBehind {
                    val stripeW = 14f
                    val gap = 14f
                    var x = -size.height
                    while (x < size.width + size.height) {
                        val p = Path().apply {
                            moveTo(x, size.height)
                            lineTo(x + size.height, 0f)
                            lineTo(x + size.height + stripeW, 0f)
                            lineTo(x + stripeW, size.height)
                            close()
                        }
                        drawPath(p, color = accent.copy(alpha = 0.16f))
                        x += stripeW + gap
                    }
                    // заполнение при hold
                    if (progress.value > 0f) {
                        drawRect(
                            color = accent.copy(alpha = 0.45f),
                            size = androidx.compose.ui.geometry.Size(size.width * progress.value, size.height),
                        )
                    }
                }
            } else {
                Modifier
            },
        )

    val clickMod = if (holdToConfirm) {
        base.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    val job = scope.launch {
                        progress.animateTo(1f, tween(1400, easing = LinearEasing))
                        onClick()
                    }
                    val released = tryAwaitRelease()
                    if (!released || progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(220)) }
                    }
                },
            )
        }
    } else {
        base.clickable { onClick() }
    }

    Box(modifier = clickMod, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = if (filled) Color.White else GlitchPalette.Phosphor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
