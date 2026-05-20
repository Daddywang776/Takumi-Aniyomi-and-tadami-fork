package eu.kanade.presentation.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.AuroraTheme
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val colors = AuroraTheme.colors
    val animatedAuroraBackground by uiPreferences.animatedAuroraBackground().collectAsState()
    val specialBackgroundStyle by uiPreferences.specialBackgroundStyle().collectAsState()

    AuroraAmbientBackground(
        enabled = animatedAuroraBackground && !colors.isEInk,
        specialBackgroundStyle = specialBackgroundStyle,
        modifier = modifier,
        content = content,
    )
}

@Composable
fun AuroraAmbientBackground(
    enabled: Boolean,
    specialBackgroundStyle: String = "none",
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AuroraTheme.colors
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            isLifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldAnimate = shouldAnimateAuroraBackground(
        userEnabled = enabled && !colors.isEInk,
        isLifecycleResumed = isLifecycleResumed,
        systemAnimationsEnabled = ValueAnimator.areAnimatorsEnabled(),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
    ) {
        if (enabled && !colors.isEInk) {
            if (specialBackgroundStyle == "none") {
                if (shouldAnimate) {
                    val transition = rememberInfiniteTransition(label = "auroraAmbient")
                    val blobOneDriftX by transition.animateFloat(
                        initialValue = -0.08f,
                        targetValue = 0.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 26000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobOneDriftX",
                    )
                    val blobOneDriftY by transition.animateFloat(
                        initialValue = -0.05f,
                        targetValue = 0.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 30000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobOneDriftY",
                    )
                    val blobTwoDriftX by transition.animateFloat(
                        initialValue = 0.08f,
                        targetValue = -0.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 22000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobTwoDriftX",
                    )
                    val blobTwoDriftY by transition.animateFloat(
                        initialValue = 0.02f,
                        targetValue = -0.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 28000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobTwoDriftY",
                    )
                    val blobThreeDriftX by transition.animateFloat(
                        initialValue = -0.03f,
                        targetValue = 0.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 20000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobThreeDriftX",
                    )
                    val blobThreeDriftY by transition.animateFloat(
                        initialValue = 0.06f,
                        targetValue = -0.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 24000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobThreeDriftY",
                    )
                    val blobOneAlpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 18000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobOneAlpha",
                    )
                    val blobTwoAlpha by transition.animateFloat(
                        initialValue = 0.12f,
                        targetValue = 0.22f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 20000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobTwoAlpha",
                    )
                    val blobThreeAlpha by transition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.18f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 24000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "blobThreeAlpha",
                    )

                    AuroraAmbientCanvas(
                        colors = colors,
                        blobOneDriftX = blobOneDriftX,
                        blobOneDriftY = blobOneDriftY,
                        blobTwoDriftX = blobTwoDriftX,
                        blobTwoDriftY = blobTwoDriftY,
                        blobThreeDriftX = blobThreeDriftX,
                        blobThreeDriftY = blobThreeDriftY,
                        blobOneAlpha = blobOneAlpha,
                        blobTwoAlpha = blobTwoAlpha,
                        blobThreeAlpha = blobThreeAlpha,
                    )
                } else {
                    AuroraAmbientCanvas(
                        colors = colors,
                        blobOneDriftX = -0.08f,
                        blobOneDriftY = -0.05f,
                        blobTwoDriftX = 0.08f,
                        blobTwoDriftY = 0.02f,
                        blobThreeDriftX = -0.03f,
                        blobThreeDriftY = 0.06f,
                        blobOneAlpha = 0.2f,
                        blobTwoAlpha = 0.12f,
                        blobThreeAlpha = 0.1f,
                    )
                }
            } else {
                AuroraSpecialBackgroundCanvas(
                    colors = colors,
                    styleKey = specialBackgroundStyle,
                    animate = shouldAnimate,
                )
            }
        }

        content()
    }
}

@Composable
private fun AuroraAmbientCanvas(
    colors: eu.kanade.presentation.theme.AuroraColors,
    blobOneDriftX: Float,
    blobOneDriftY: Float,
    blobTwoDriftX: Float,
    blobTwoDriftY: Float,
    blobThreeDriftX: Float,
    blobThreeDriftY: Float,
    blobOneAlpha: Float,
    blobTwoAlpha: Float,
    blobThreeAlpha: Float,
) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        fun drawAmbientBlob(
            centerFractionX: Float,
            centerFractionY: Float,
            driftX: Float,
            driftY: Float,
            radiusFraction: Float,
            aspectX: Float,
            aspectY: Float,
            color: Color,
            alpha: Float,
        ) {
            val center = Offset(
                x = size.width * (centerFractionX + driftX),
                y = size.height * (centerFractionY + driftY),
            )
            val radius = size.minDimension * radiusFraction
            withTransform({
                translate(
                    left = center.x - radius * aspectX,
                    top = center.y - radius * aspectY,
                )
                scale(scaleX = aspectX, scaleY = aspectY, pivot = Offset.Zero)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            Color.Transparent,
                        ),
                        center = Offset(radius, radius),
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
        }

        val blobBaseAlpha = if (colors.isDark) 1f else 0.45f
        drawAmbientBlob(
            centerFractionX = 0.22f,
            centerFractionY = 0.14f,
            driftX = blobOneDriftX,
            driftY = blobOneDriftY,
            radiusFraction = 0.5f,
            aspectX = 1.35f,
            aspectY = 0.8f,
            color = colors.glowEffect,
            alpha = blobOneAlpha * blobBaseAlpha,
        )
        drawAmbientBlob(
            centerFractionX = 0.84f,
            centerFractionY = 0.22f,
            driftX = blobTwoDriftX,
            driftY = blobTwoDriftY,
            radiusFraction = 0.44f,
            aspectX = 1.15f,
            aspectY = 0.72f,
            color = colors.gradientPurple,
            alpha = blobTwoAlpha * blobBaseAlpha,
        )
        drawAmbientBlob(
            centerFractionX = 0.48f,
            centerFractionY = 0.72f,
            driftX = blobThreeDriftX,
            driftY = blobThreeDriftY,
            radiusFraction = 0.52f,
            aspectX = 1.45f,
            aspectY = 0.9f,
            color = colors.accent,
            alpha = blobThreeAlpha * 0.72f,
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    colors.background.copy(alpha = if (colors.isDark) 0.2f else 0.12f),
                    colors.background.copy(alpha = if (colors.isDark) 0.38f else 0.2f),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                radius = size.maxDimension * 0.92f,
            ),
        )
    }
}

@Composable
private fun AuroraSpecialBackgroundCanvas(
    colors: eu.kanade.presentation.theme.AuroraColors,
    styleKey: String,
    animate: Boolean,
) {
    if (styleKey == "none" || colors.isEInk) return

    val drift = if (animate) {
        val transition = rememberInfiniteTransition(label = "auroraSpecialBackground")
        transition.animateFloat(
            initialValue = -0.08f,
            targetValue = 0.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "auroraSpecialDrift",
        ).value
    } else {
        0f
    }
    val spin = if (animate) {
        val transition = rememberInfiniteTransition(label = "auroraSpecialSpin")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 22000)),
            label = "auroraSpecialSpinValue",
        ).value
    } else {
        0f
    }
    val pulse = if (animate) {
        val transition = rememberInfiniteTransition(label = "auroraSpecialPulse")
        transition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "auroraSpecialPulseValue",
        ).value
    } else {
        0.85f
    }
    val cometProgressOne = if (animate) {
        val transition = rememberInfiniteTransition(label = "auroraSpecialCometOne")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 22000,
                    delayMillis = 5000,
                ),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(1800, StartOffsetType.FastForward),
            ),
            label = "auroraSpecialCometOneValue",
        ).value
    } else {
        0f
    }
    val cometProgressTwo = if (animate) {
        val transition = rememberInfiniteTransition(label = "auroraSpecialCometTwo")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 28000,
                    delayMillis = 9000,
                ),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(9600, StartOffsetType.FastForward),
            ),
            label = "auroraSpecialCometTwoValue",
        ).value
    } else {
        0f
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        when (styleKey) {
            "petal_storm" -> {
                val petalColor = if (colors.isDark) {
                    Color(0xFFFFA7C8)
                } else {
                    Color(0xFFFF84B7)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD9E8).copy(alpha = 0.16f * pulse),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.2f),
                        radius = size.minDimension * 0.78f,
                    ),
                )
                listOf(
                    Offset(size.width * 0.18f, size.height * 0.12f),
                    Offset(size.width * 0.72f, size.height * 0.18f),
                    Offset(size.width * 0.35f, size.height * 0.4f),
                    Offset(size.width * 0.86f, size.height * 0.55f),
                    Offset(size.width * 0.12f, size.height * 0.7f),
                ).forEachIndexed { index, offset ->
                    val xDrift = drift * (index + 1) * 0.12f
                    val yDrift = drift * (index + 1) * 0.08f
                    val rotation = spin + (index * 28f)
                    val petalLength = 28f + (index * 4f)
                    val petalWidth = 10f + (index * 1.8f)
                    val path = Path().apply {
                        moveTo(0f, -petalLength * 0.5f)
                        quadraticTo(
                            petalWidth * 0.9f,
                            -petalLength * 0.28f,
                            petalWidth * 0.55f,
                            petalLength * 0.1f,
                        )
                        quadraticTo(
                            petalWidth * 0.2f,
                            petalLength * 0.5f,
                            0f,
                            petalLength * 0.5f,
                        )
                        quadraticTo(
                            -petalWidth * 0.2f,
                            petalLength * 0.5f,
                            -petalWidth * 0.55f,
                            petalLength * 0.1f,
                        )
                        quadraticTo(
                            -petalWidth * 0.9f,
                            -petalLength * 0.28f,
                            0f,
                            -petalLength * 0.5f,
                        )
                        close()
                    }
                    withTransform({
                        translate(
                            left = offset.x + (size.width * xDrift),
                            top = offset.y + (size.height * yDrift),
                        )
                        rotate(rotation, pivot = Offset.Zero)
                    }) {
                        drawPath(
                            path = path,
                            color = petalColor.copy(alpha = 0.22f + (0.06f * index)),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = petalWidth * 0.18f,
                            center = Offset(0f, -petalLength * 0.15f),
                        )
                    }
                }
            }
            "neon_orbit" -> {
                val ringColors = listOf(
                    Color(0xFF49E6FF),
                    Color(0xFF9B8CFF),
                    colors.accent,
                )
                val center = Offset(size.width * 0.52f, size.height * 0.46f)
                listOf(
                    Offset(0.18f, 0.22f),
                    Offset(0.74f, 0.2f),
                    Offset(0.84f, 0.68f),
                    Offset(0.22f, 0.78f),
                ).forEachIndexed { index, fraction ->
                    val orbitRadius = size.minDimension * (0.18f + (index * 0.08f))
                    val angle = Math.toRadians((spin + index * 88f).toDouble())
                    val x = center.x + kotlin.math.cos(angle).toFloat() * orbitRadius
                    val y = center.y + kotlin.math.sin(angle).toFloat() * orbitRadius
                    drawCircle(
                        color = ringColors[index % ringColors.size].copy(alpha = 0.55f + (0.1f * pulse)),
                        radius = 5f + index,
                        center = Offset(x + (size.width * drift * fraction.x), y + (size.height * drift * fraction.y)),
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6EF6FF).copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension * 0.55f,
                    ),
                )

                fun drawComet(
                    progress: Float,
                    start: Offset,
                    end: Offset,
                    color: Color,
                ) {
                    val visibility = when {
                        progress < 0.08f -> progress / 0.08f
                        progress < 0.15f -> 1f - ((progress - 0.08f) / 0.07f)
                        else -> 0f
                    }
                    if (visibility <= 0f) return

                    val head = Offset(
                        x = start.x + ((end.x - start.x) * progress),
                        y = start.y + ((end.y - start.y) * progress),
                    )
                    val tailLength = size.minDimension * (0.16f + (0.04f * progress))
                    val tailDirection = Offset(
                        x = -(end.x - start.x),
                        y = -(end.y - start.y),
                    )
                    val magnitude = kotlin.math.sqrt(
                        (tailDirection.x * tailDirection.x) + (tailDirection.y * tailDirection.y),
                    ).coerceAtLeast(1f)
                    val tailUnit = Offset(
                        x = tailDirection.x / magnitude,
                        y = tailDirection.y / magnitude,
                    )
                    val tail = Offset(
                        x = head.x + (tailUnit.x * tailLength),
                        y = head.y + (tailUnit.y * tailLength),
                    )

                    drawLine(
                        color = color.copy(alpha = 0.06f + (0.12f * visibility)),
                        start = tail,
                        end = head,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f * visibility),
                        start = Offset(
                            x = tail.x + (tailUnit.x * tailLength * 0.22f),
                            y = tail.y + (tailUnit.y * tailLength * 0.22f),
                        ),
                        end = head,
                        strokeWidth = 1.4f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f * visibility),
                        radius = 3.5f,
                        center = head,
                    )
                }

                drawComet(
                    progress = cometProgressOne,
                    start = Offset(-size.width * 0.12f, size.height * 0.18f),
                    end = Offset(size.width * 1.08f, size.height * 0.76f),
                    color = Color(0xFFBDFBFF),
                )
                drawComet(
                    progress = cometProgressTwo,
                    start = Offset(-size.width * 0.08f, size.height * 0.82f),
                    end = Offset(size.width * 1.1f, size.height * 0.14f),
                    color = Color(0xFF9B8CFF),
                )
            }
        }
    }
}

internal fun shouldAnimateAuroraBackground(
    userEnabled: Boolean,
    isLifecycleResumed: Boolean,
    systemAnimationsEnabled: Boolean,
): Boolean {
    return userEnabled && isLifecycleResumed && systemAnimationsEnabled
}

@Composable
fun AuroraHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    // Reusable header component if needed, currently TopBars are custom per screen
}
