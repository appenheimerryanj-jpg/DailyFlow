package com.appenheimer.dailyflow.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.TaskPriority
import com.appenheimer.dailyflow.model.safePriority
import com.appenheimer.dailyflow.model.safeText

enum class FlowPose {
    HAPPY,
    CHECKLIST,
    NOTE,
    CELEBRATE,
    PREMIUM,
    EMPTY
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

@Composable
fun FlowMascot(
    pose: FlowPose,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.size(96.dp)) {
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
        drawPath(body, tint)
        drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.42f, h * 0.53f))
        drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.58f, h * 0.53f))
        drawArc(
            color = Color.White,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.40f, h * 0.58f),
            size = Size(w * 0.20f, h * 0.12f),
            style = Stroke(width = w * 0.026f, cap = StrokeCap.Round)
        )

        when (pose) {
            FlowPose.HAPPY -> {
                drawCircle(Color.White.copy(alpha = 0.28f), radius = w * 0.11f, center = Offset(w * 0.34f, h * 0.35f))
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
            }
            FlowPose.CELEBRATE -> {
                drawCircle(Color(0xFFFFD166), radius = w * 0.035f, center = Offset(w * 0.23f, h * 0.20f))
                drawCircle(Color(0xFF5D63C8), radius = w * 0.028f, center = Offset(w * 0.78f, h * 0.22f))
                drawLine(Color.White, Offset(w * 0.26f, h * 0.42f), Offset(w * 0.14f, h * 0.33f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.74f, h * 0.42f), Offset(w * 0.88f, h * 0.32f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
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
            }
            FlowPose.EMPTY -> {
                drawRoundRect(Color.White.copy(alpha = 0.82f), topLeft = Offset(w * 0.20f, h * 0.78f), size = Size(w * 0.60f, h * 0.08f), cornerRadius = CornerRadius(20f, 20f))
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, pose: FlowPose = FlowPose.EMPTY) {
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
                    FlowMascot(FlowPose.EMPTY, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.error)
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
