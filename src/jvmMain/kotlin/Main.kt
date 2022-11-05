import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.math.sin
import kotlin.random.Random

@Composable
@Preview
fun App() {
    MaterialTheme {
        val seed = remember { System.currentTimeMillis().toInt() }
        var period by remember { mutableStateOf(0f) }
        FrameEffect { time ->
            period = time
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawSand(
                        seed = seed,
                        curve = Line(
                            start = Offset(0.0f, 0.0f),
                            end = Offset(size.width, size.height)
                        ),
                        time = period
                    )
                }
        ) {
            Image(
                painter = painterResource("title.png"),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

interface Curve {
    val length: Float
    fun value(t: Float): Offset
    fun direction(t: Float): Offset
    fun normal(t: Float): Offset
}

class Line(
    private val start: Offset,
    private val end: Offset,
) : Curve {
    private val direction = (end - start) / length
    private val normal = Offset(-direction.y, direction.x)

    override val length: Float get() = (end - start).getDistance()

    override fun value(t: Float): Offset = start + direction * t * length

    override fun direction(t: Float): Offset = direction

    override fun normal(t: Float): Offset = normal
}

fun DrawScope.drawSand(
    seed: Int,
    curve: Curve,
    time: Float,
) {
    val random = Random(seed)
    val color = Color.White
    val numParticles = (curve.length * 0.1f).toInt()
    val directionalVariance = (1.0 / numParticles) / 2.0
    repeat(numParticles) {
        val t = it.toFloat() / numParticles
        repeat(4) {
            val sinus = 3.0f * sin((300.0f * t + time) % 1000f)
            val normalShift = random.nextDouble(-10.0, 10.0).toFloat() + sinus
            val directionalValue = random.nextDouble(-directionalVariance, directionalVariance).toFloat()
            val radius = random.nextDouble(2.0, 4.0).toFloat()
            val curTValue = t + directionalValue
            val curOffset = curve.value(curTValue) + curve.normal(curTValue) * normalShift
            drawCircle(color, radius, curOffset)
        }
    }
}

@Composable
@NonRestartableComposable
fun FrameEffect(
    block: (period: Float) -> Unit
) {
    LaunchedEffect(Unit) {
        var firstFrame = 0L
        while (isActive) {
            withFrameNanos { nextFrame ->
                if (firstFrame != 0L) {
                    block.invoke((nextFrame - firstFrame).toFloat() / 1000000000f)
                } else {
                    firstFrame = nextFrame
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun awaitFrame() =
    suspendCancellableCoroutine { cont ->
        with(cont) {
            Dispatchers.Main.resumeUndispatched(System.nanoTime())
        }
    }

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "The Rings of Power",
        state = rememberWindowState(width = 1280.dp, height = 720.dp),
        resizable = false,
    ) {
        App()
    }
}
