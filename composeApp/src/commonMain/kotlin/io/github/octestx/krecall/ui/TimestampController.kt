import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampRateController(
    timestamps: List<Long>,
    currentTimestamp: MutableState<Long>,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var currentIndex by remember { mutableIntStateOf(timestamps.lastIndex) }
    var isDragging by remember { mutableStateOf(false) }
    val maxMultiplier = 15f

    // 计算速度倍率（-15x 到 +15x）
    val speedMultiplier = remember(sliderPosition) {
        (sliderPosition - 0.5f) * 2 * maxMultiplier
    }

    // 自动滚动协程
    LaunchedEffect(isDragging, speedMultiplier) {
        if (isDragging && timestamps.isNotEmpty()) {
            var lastUpdateTime = 0L
            while (true) {
                val interval = (50 / abs(speedMultiplier)).toLong().coerceAtLeast(50)
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastUpdateTime >= interval) {
                    val direction = sign(speedMultiplier)
                    val newIndex = (currentIndex + direction).coerceIn(0, timestamps.lastIndex)

                    if (newIndex != currentIndex) {
                        currentIndex = newIndex
                        currentTimestamp.value = timestamps[newIndex]
                    }

                    lastUpdateTime = currentTime
                }
                delay(16) // 约60fps更新频率
            }
        }
    }

    Column(modifier.padding(16.dp)) {
        // 倍率指示器
        Text(
            text = "×${abs(speedMultiplier).format(1)} ${if(speedMultiplier>0)"▶" else "◀"}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )

        // 滑动控制器
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                isDragging = true
            },
            onValueChangeFinished = {
                sliderPosition = 0.5f
                isDragging = false
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            },
            track = { sliderPositions ->
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val background = MaterialTheme.colorScheme.surfaceVariant
                    val line = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val trackWidth = size.width
                        val activeOffset = sliderPositions.valueRange.endInclusive * trackWidth

                        // 绘制背景轨道
                        drawLine(
                            color = background,
                            start = Offset(0f, center.y),
                            end = Offset(trackWidth, center.y),
                            strokeWidth = 8.dp.toPx()
                        )

                        // 绘制激活轨道
                        drawLine(
                            color = line,
                            start = Offset(center.x - trackWidth/4, center.y),
                            end = Offset(center.x + trackWidth/4, center.y),
                            strokeWidth = 12.dp.toPx()
                        )
                    }
                }
            }
        )

        // 时间戳显示
        Text(
            text = "Selected: ${currentTimestamp.value}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 辅助扩展函数
private fun Float.format(decimal: Int): String = "%.${decimal}f".format(this)
private fun sign(value: Float): Int = if (value > 0) 1 else if (value < 0) -1 else 0
