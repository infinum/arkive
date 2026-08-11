@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Moss
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineLight
import com.infinum.arkive.sample.theme.Slate

@Composable
fun ProgressRing(
    progress: Float,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Moss,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Pine, PineLight, Pine)),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(text = caption, style = MaterialTheme.typography.labelSmall, color = Slate)
        }
    }
}

@Composable
fun WeeklyBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = values.size - 1,
) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = modifier
            .background(color = Cloud, shape = RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.take(labels.size).forEachIndexed { index, value ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((90 * value).dp + 8.dp)
                        .background(
                            color = if (index == highlightIndex) Pine else Moss,
                            shape = CircleShape,
                        ),
                )
                Box(modifier = Modifier.height(8.dp))
                Text(text = labels[index], style = MaterialTheme.typography.labelSmall, color = Slate)
            }
        }
    }
}

@Composable
fun ElevationChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .width(320.dp)
            .height(120.dp),
    ) {
        drawElevationProfile(points)
    }
}

private fun DrawScope.drawElevationProfile(points: List<Float>) {
    if (points.size >= 2) {
        val stepX = size.width / (points.size - 1)
        val line = Path()
        val area = Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height * (1f - point)
            if (index == 0) {
                line.moveTo(x, y)
                area.moveTo(x, size.height)
                area.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(size.width, size.height)
        area.close()
        drawPath(
            path = area,
            brush = Brush.verticalGradient(listOf(PineLight.copy(alpha = 0.35f), PineLight.copy(alpha = 0.02f))),
        )
        drawPath(
            path = line,
            color = Pine,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = Pine,
            radius = 5.dp.toPx(),
            center = Offset(size.width, size.height * (1f - points.last())),
        )
    }
}

@Preview
@ArkiveComposable(name = "Progress Ring", group = "Progress", tags = ["chart"])
@Composable
internal fun PreviewProgressRing() {
    PeakTheme {
        ProgressRing(progress = 0.72f, value = "8,412", caption = "of 10,000 steps")
    }
}

@Preview
@ArkiveComposable(name = "Weekly Bar Chart", group = "Progress", tags = ["chart"])
@Composable
internal fun PreviewWeeklyBarChart() {
    PeakTheme {
        WeeklyBarChart(values = listOf(0.4f, 0.7f, 0.3f, 0.85f, 0.55f, 1f, 0.62f))
    }
}

@Preview
@ArkiveComposable(name = "Elevation Chart", group = "Progress", tags = ["chart"])
@Composable
internal fun PreviewElevationChart() {
    PeakTheme {
        ElevationChart(points = listOf(0.1f, 0.25f, 0.2f, 0.45f, 0.6f, 0.5f, 0.8f, 0.95f, 0.7f, 0.75f))
    }
}
