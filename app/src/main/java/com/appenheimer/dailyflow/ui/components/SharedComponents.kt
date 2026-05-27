package com.appenheimer.dailyflow.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.DelightEvent
import com.appenheimer.dailyflow.model.DelightType
import com.appenheimer.dailyflow.model.TaskPriority
import com.appenheimer.dailyflow.model.safePriority
import com.appenheimer.dailyflow.model.safeText
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class FlowPose {
    HAPPY,
    CHECKLIST,
    NOTE,
    CELEBRATE,
    PREMIUM,
    THINKING,
    ENCOURAGING,
    CALM,
    LIMIT,
    MUSIC,
    EMPTY
}

enum class FlowMotion {
    IDLE,
    HAPPY,
    CELEBRATE,
    THINKING,
    ENCOURAGING,
    PREMIUM,
    CALM,
    MUSIC
}

@Composable
fun ScreenList(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    actionText: String? = null,
    actionIcon: ImageVector = Icons.Filled.Add,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionText)
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 8 }
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (actionLabel != null && onAction != null) {
                        TextButton(onClick = onAction) { Text(actionLabel) }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
        }
    }
}

@Composable
fun FlowMascot(
    pose: FlowPose,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    motion: FlowMotion = pose.defaultMotion(),
    reducedMotion: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "FlowMotion")
    val breath by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "FlowBreath"
    )
    val bob by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "FlowBob"
    )
    val sway by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "FlowSway"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "FlowPulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "FlowGlow"
    )

    val offsetY = if (reducedMotion) 0f else when (motion) {
        FlowMotion.CELEBRATE, FlowMotion.HAPPY -> bob * 1.35f
        FlowMotion.MUSIC -> bob * 0.8f
        FlowMotion.IDLE, FlowMotion.ENCOURAGING, FlowMotion.PREMIUM, FlowMotion.CALM -> bob
        FlowMotion.THINKING -> 0f
    }
    val rotation = if (reducedMotion) 0f else when (motion) {
        FlowMotion.THINKING -> sway
        FlowMotion.ENCOURAGING -> sway * 0.55f
        FlowMotion.MUSIC -> sway * 0.35f
        else -> 0f
    }
    val scale = if (reducedMotion) 1f else when (motion) {
        FlowMotion.HAPPY, FlowMotion.CELEBRATE -> 1f + pulse * 0.06f
        FlowMotion.MUSIC -> 1f + pulse * 0.035f
        FlowMotion.CALM -> 0.99f + pulse * 0.012f
        FlowMotion.PREMIUM -> 1f + pulse * 0.025f
        else -> breath
    }

    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        if (motion == FlowMotion.PREMIUM && !reducedMotion) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(tint.copy(alpha = glowAlpha), radius = size.minDimension * 0.48f, center = Offset(size.width / 2f, size.height / 2f))
                drawCircle(Color.White.copy(alpha = glowAlpha * 0.6f), radius = size.minDimension * 0.32f, center = Offset(size.width * 0.36f, size.height * 0.32f))
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = offsetY
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                cubicTo(w * 0.22f, h * 0.34f, w * 0.18f, h * 0.53f, w * 0.20f, h * 0.66f)
                cubicTo(w * 0.23f, h * 0.86f, w * 0.37f, h * 0.96f, w * 0.50f, h * 0.96f)
                cubicTo(w * 0.63f, h * 0.96f, w * 0.77f, h * 0.86f, w * 0.80f, h * 0.66f)
                cubicTo(w * 0.82f, h * 0.53f, w * 0.78f, h * 0.34f, w * 0.50f, h * 0.08f)
                close()
            }
            drawRoundRect(Color(0xFF003F47).copy(alpha = 0.14f), topLeft = Offset(w * 0.23f, h * 0.84f), size = Size(w * 0.54f, h * 0.10f), cornerRadius = CornerRadius(w * 0.18f, h * 0.08f))
            drawPath(body, tint)
            drawPath(body, Color.White.copy(alpha = 0.08f))
            drawCircle(Color.White.copy(alpha = 0.30f), radius = w * 0.12f, center = Offset(w * 0.35f, h * 0.34f))
            drawCircle(Color(0xFF004F59).copy(alpha = 0.18f), radius = w * 0.19f, center = Offset(w * 0.62f, h * 0.72f))

            val faceColor = Color(0xFF063D45)
            val leftEye = Offset(w * 0.42f, h * 0.52f)
            val rightEye = Offset(w * 0.58f, h * 0.52f)
            if (pose == FlowPose.CALM) {
                drawArc(faceColor, 15f, 150f, false, Offset(w * 0.37f, h * 0.49f), Size(w * 0.10f, h * 0.08f), style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
                drawArc(faceColor, 15f, 150f, false, Offset(w * 0.53f, h * 0.49f), Size(w * 0.10f, h * 0.08f), style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
            } else {
                drawCircle(Color.White, radius = w * 0.058f, center = leftEye)
                drawCircle(Color.White, radius = w * 0.058f, center = rightEye)
                val eyeLift = if (pose == FlowPose.THINKING) -h * 0.018f else 0f
                val eyeSide = if (pose == FlowPose.LIMIT) -w * 0.012f else 0f
                drawCircle(faceColor, radius = w * 0.026f, center = Offset(leftEye.x + eyeSide, leftEye.y + eyeLift))
                drawCircle(faceColor, radius = w * 0.026f, center = Offset(rightEye.x + eyeSide, rightEye.y + eyeLift))
                drawCircle(Color.White.copy(alpha = 0.86f), radius = w * 0.009f, center = Offset(leftEye.x - w * 0.010f + eyeSide, leftEye.y - h * 0.010f + eyeLift))
                drawCircle(Color.White.copy(alpha = 0.86f), radius = w * 0.009f, center = Offset(rightEye.x - w * 0.010f + eyeSide, rightEye.y - h * 0.010f + eyeLift))
            }
            drawCircle(Color.White.copy(alpha = 0.22f), radius = w * 0.030f, center = Offset(w * 0.34f, h * 0.60f))
            drawCircle(Color.White.copy(alpha = 0.22f), radius = w * 0.030f, center = Offset(w * 0.66f, h * 0.60f))
            drawArc(
                color = faceColor,
                startAngle = 22f,
                sweepAngle = if (pose == FlowPose.LIMIT) 96f else 136f,
                useCenter = false,
                topLeft = Offset(w * 0.40f, h * 0.60f),
                size = Size(w * 0.20f, h * 0.12f),
                style = Stroke(width = w * (0.020f + pulse * 0.003f), cap = StrokeCap.Round)
            )

            when (pose) {
                FlowPose.HAPPY -> {
                    drawCircle(Color.White.copy(alpha = 0.28f + pulse * 0.08f), radius = w * 0.11f, center = Offset(w * 0.34f, h * 0.35f))
                }
                FlowPose.CHECKLIST -> {
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.62f, h * 0.25f), size = Size(w * 0.28f, h * 0.34f), cornerRadius = CornerRadius(8f, 8f))
                    repeat(3) { index ->
                        val y = h * (0.32f + index * 0.08f)
                        drawLine(tint, Offset(w * 0.67f, y), Offset(w * 0.71f, y + h * 0.03f), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
                        drawLine(tint, Offset(w * 0.71f, y + h * 0.03f), Offset(w * 0.80f, y - h * 0.03f), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
                    }
                }
                FlowPose.NOTE -> {
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.62f, h * 0.25f), size = Size(w * 0.27f, h * 0.30f), cornerRadius = CornerRadius(8f, 8f))
                    repeat(3) { index ->
                        val y = h * (0.34f + index * 0.07f)
                        drawLine(tint, Offset(w * 0.67f, y), Offset(w * 0.82f, y), strokeWidth = w * 0.014f, cap = StrokeCap.Round)
                    }
                    drawCircle(tint.copy(alpha = 0.20f + pulse * 0.18f), radius = w * 0.20f, center = Offset(w * 0.75f, h * 0.40f), style = Stroke(width = w * 0.018f))
                }
                FlowPose.CELEBRATE -> {
                    drawCircle(Color(0xFFFFD166), radius = w * 0.035f, center = Offset(w * 0.23f, h * 0.20f))
                    drawCircle(Color(0xFF5D63C8), radius = w * 0.028f, center = Offset(w * 0.78f, h * 0.22f))
                    drawLine(Color.White, Offset(w * 0.26f, h * 0.42f), Offset(w * 0.14f, h * 0.33f - pulse * h * 0.04f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(w * 0.74f, h * 0.42f), Offset(w * 0.88f, h * 0.32f - pulse * h * 0.04f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
                }
                FlowPose.PREMIUM -> {
                    val crown = Path().apply {
                        moveTo(w * 0.33f, h * 0.23f)
                        lineTo(w * 0.43f, h * 0.13f)
                        lineTo(w * 0.50f, h * 0.25f)
                        lineTo(w * 0.60f, h * 0.13f)
                        lineTo(w * 0.70f, h * 0.23f)
                        lineTo(w * 0.66f, h * 0.32f)
                        lineTo(w * 0.36f, h * 0.32f)
                        close()
                    }
                    drawPath(crown, Color(0xFFFFC857))
                    drawLine(Color.White.copy(alpha = 0.75f), Offset(w * (0.24f + pulse * 0.20f), h * 0.18f), Offset(w * (0.38f + pulse * 0.20f), h * 0.08f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
                }
                FlowPose.THINKING -> {
                    repeat(3) { index ->
                        val radius = w * (0.025f + index * 0.012f)
                        drawCircle(Color.White.copy(alpha = 0.50f - index * 0.08f), radius = radius, center = Offset(w * (0.64f + index * 0.08f), h * (0.25f - index * 0.05f + pulse * 0.02f)))
                    }
                }
                FlowPose.ENCOURAGING -> {
                    drawLine(Color.White, Offset(w * 0.25f, h * 0.47f), Offset(w * (0.12f + pulse * 0.03f), h * (0.39f - pulse * 0.05f)), strokeWidth = w * 0.024f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(w * 0.75f, h * 0.47f), Offset(w * 0.88f, h * 0.39f), strokeWidth = w * 0.024f, cap = StrokeCap.Round)
                    drawCircle(Color.White.copy(alpha = 0.18f + pulse * 0.18f), radius = w * (0.18f + pulse * 0.04f), center = Offset(w * 0.50f, h * 0.54f), style = Stroke(width = w * 0.018f))
                }
                FlowPose.CALM -> {
                    drawRoundRect(Color.White.copy(alpha = 0.32f), topLeft = Offset(w * 0.27f, h * 0.72f), size = Size(w * 0.46f, h * 0.08f), cornerRadius = CornerRadius(20f, 20f))
                    drawCircle(Color.White.copy(alpha = 0.18f), radius = w * 0.09f, center = Offset(w * 0.24f, h * 0.25f + pulse * h * 0.015f))
                }
                FlowPose.LIMIT -> {
                    drawRoundRect(Color.White, topLeft = Offset(w * 0.62f, h * 0.28f), size = Size(w * 0.24f, h * 0.24f), cornerRadius = CornerRadius(12f, 12f))
                    drawLine(tint, Offset(w * 0.74f, h * 0.34f), Offset(w * 0.74f, h * 0.42f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
                    drawCircle(tint, radius = w * 0.012f, center = Offset(w * 0.74f, h * 0.47f))
                    drawLine(Color.White, Offset(w * 0.24f, h * 0.48f), Offset(w * 0.14f, h * 0.54f), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
                }
                FlowPose.MUSIC -> {
                    repeat(4) { index ->
                        val x = w * (0.62f + index * 0.055f)
                        val bar = h * (0.08f + ((index + 1) % 3) * 0.035f + pulse * 0.04f)
                        drawRoundRect(Color.White.copy(alpha = 0.90f), topLeft = Offset(x, h * 0.50f - bar), size = Size(w * 0.030f, bar), cornerRadius = CornerRadius(8f, 8f))
                    }
                    drawCircle(Color.White.copy(alpha = 0.35f + pulse * 0.18f), radius = w * 0.20f, center = Offset(w * 0.50f, h * 0.52f), style = Stroke(width = w * 0.016f))
                }
                FlowPose.EMPTY -> {
                    drawRoundRect(Color.White.copy(alpha = 0.82f), topLeft = Offset(w * 0.20f, h * 0.78f), size = Size(w * 0.60f, h * 0.08f), cornerRadius = CornerRadius(20f, 20f))
                }
            }
        }
        if (motion == FlowMotion.CELEBRATE && !reducedMotion) {
            AmbientSparkles(modifier = Modifier.fillMaxSize(), alpha = 0.45f + pulse * 0.35f)
        }
    }
}

private fun FlowPose.defaultMotion(): FlowMotion = when (this) {
    FlowPose.HAPPY -> FlowMotion.HAPPY
    FlowPose.CHECKLIST, FlowPose.NOTE -> FlowMotion.IDLE
    FlowPose.CELEBRATE -> FlowMotion.CELEBRATE
    FlowPose.PREMIUM -> FlowMotion.PREMIUM
    FlowPose.THINKING -> FlowMotion.THINKING
    FlowPose.ENCOURAGING, FlowPose.EMPTY -> FlowMotion.ENCOURAGING
    FlowPose.CALM -> FlowMotion.CALM
    FlowPose.LIMIT -> FlowMotion.ENCOURAGING
    FlowPose.MUSIC -> FlowMotion.MUSIC
}

@Composable
private fun AmbientSparkles(modifier: Modifier = Modifier, alpha: Float) {
    Canvas(modifier = modifier) {
        val sparkleColor = Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
        val points = listOf(
            Offset(size.width * 0.18f, size.height * 0.18f),
            Offset(size.width * 0.82f, size.height * 0.24f),
            Offset(size.width * 0.74f, size.height * 0.78f)
        )
        points.forEachIndexed { index, center ->
            val radius = size.minDimension * (0.025f + index * 0.006f)
            drawLine(sparkleColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = radius * 0.38f, cap = StrokeCap.Round)
            drawLine(sparkleColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = radius * 0.38f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun SparkleBurst(
    trigger: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var active by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "SparkleBurst"
    )

    LaunchedEffect(trigger) {
        if (trigger == 0L) return@LaunchedEffect
        active = false
        delay(24)
        active = true
        delay(680)
        active = false
    }

    Canvas(modifier = modifier) {
        if (progress <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val radius = size.minDimension * (0.10f + progress * 0.40f)
        repeat(9) { index ->
            val angle = (PI * 2.0 * index / 9.0).toFloat()
            val end = Offset(
                center.x + cos(angle) * radius,
                center.y + sin(angle) * radius
            )
            val dotSize = size.minDimension * (0.018f + (index % 3) * 0.006f)
            val sparkleColor = when (index % 3) {
                0 -> color
                1 -> Color(0xFFFFD166)
                else -> Color(0xFF5D63C8)
            }.copy(alpha = alpha)
            drawCircle(sparkleColor, radius = dotSize, center = end)
        }
    }
}

@Composable
fun FlowDelightOverlay(event: DelightEvent?, reducedMotion: Boolean = false, onFinished: () -> Unit) {
    var lastEvent by remember { mutableStateOf<DelightEvent?>(null) }
    LaunchedEffect(event?.createdAt) {
        if (event != null) {
            lastEvent = event
            delay(1850)
            onFinished()
        }
    }
    val current = lastEvent
    AnimatedVisibility(
        visible = event != null && current != null,
        enter = fadeIn(tween(160)) + scaleIn(tween(180), initialScale = 0.92f) + slideInVertically(tween(220)) { -it / 2 },
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f) + slideOutVertically(tween(180)) { -it / 2 }
    ) {
        if (current != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ElevatedCard(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            FlowMascot(current.type.delightPose(), modifier = Modifier.size(68.dp), motion = FlowMotion.CELEBRATE, reducedMotion = reducedMotion)
                            if (!reducedMotion) SparkleBurst(trigger = current.createdAt, modifier = Modifier.size(92.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Flow noticed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(current.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

private fun DelightType.delightPose(): FlowPose = when (this) {
    DelightType.FIRST_TASK -> FlowPose.CHECKLIST
    DelightType.FIRST_NOTE -> FlowPose.NOTE
    DelightType.PREMIUM_UNLOCK -> FlowPose.PREMIUM
    DelightType.FOCUS_START -> FlowPose.MUSIC
    DelightType.LIMIT_REACHED -> FlowPose.LIMIT
    DelightType.FIRST_HABIT, DelightType.HABIT_COMPLETE -> FlowPose.ENCOURAGING
    DelightType.TASK_COMPLETE,
    DelightType.CLEAR_COMPLETED,
    DelightType.ALL_HABITS_DONE,
    DelightType.NEW_BEST_STREAK,
    DelightType.DAILY_GOAL_REACHED -> FlowPose.CELEBRATE
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, pose: FlowPose = FlowPose.EMPTY) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(280)) { it / 6 }
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowMascot(pose = pose, modifier = Modifier.size(84.dp))
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LimitCard(title: String, used: Int, limit: Int, premium: Boolean, message: String) {
    if (premium) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Premium active: unlimited items are enabled.", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }
    val progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = if (used >= limit) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$used/$limit", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            if (used >= limit) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowMascot(FlowPose.LIMIT, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.error, motion = FlowMotion.ENCOURAGING)
                    Spacer(Modifier.width(8.dp))
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "DailyFlowProgress"
    )
    LinearProgressIndicator(progress = { animated }, modifier = modifier)
}

@Composable
fun FocusVisualizer(active: Boolean, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "FocusVisualizer")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "FocusPhase"
    )
    Canvas(modifier = modifier.height(42.dp).fillMaxWidth()) {
        val bars = 12
        val gap = size.width / (bars * 2f)
        val barWidth = gap * 0.76f
        repeat(bars) { index ->
            val wave = if (!active || reducedMotion) 0.45f else 0.25f + 0.60f * (((index % 4) + 1) / 4f) * (0.55f + phase * 0.45f)
            val barHeight = size.height * wave.coerceIn(0.18f, 0.95f)
            val x = gap * 0.75f + index * gap * 2f
            drawRoundRect(
                color = if (active) Color(0xFF00A6B8).copy(alpha = 0.78f) else Color(0xFF6C7D80).copy(alpha = 0.32f),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
    val color = when (priority) {
        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        TaskPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
        Text(
            priority.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TaskCompactRow(task: DailyTask, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PriorityBadge(task.safePriority())
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.safeText(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (!task.dueDate.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Due ${task.dueDate}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
}

@Composable
fun StatChip(text: String) {
    AssistChip(onClick = {}, enabled = false, label = { Text(text) })
}
