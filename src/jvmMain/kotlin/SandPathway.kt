import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class SandParticle(
    val t: Float,
    val velocity: Float,
    val sandPathway: SandPathway,
    val color: Color,
) {
    companion object {
        fun sandColor(): Color =
            when (Random.nextInt(5)) {
                0 -> Color(97, 89, 75)
                1 -> Color(93, 80, 67)
                2 -> Color(91, 81, 70)
                3 -> Color(81, 72, 62)
                else -> Color(92, 81, 69)
            }
    }
}

class SandPathway(
    private val factors: FloatArray,
) {
    fun value(t: Float): Float =
        listOf(
            sin(factors[0] * (factors[1] * PI * t).toFloat() + factors[2]),
            1.0f / exp(sin(factors[3] * (factors[4] * PI * t).toFloat() + factors[5])),
            sin(factors[6] * (factors[7] * PI * t).toFloat() + factors[8]),
            1.0f / exp(sin(factors[9] * (factors[10] * PI * t).toFloat() + factors[11])),
        ).average().toFloat()

    companion object {
        fun randomizedFactors() = listOf(
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(-PI, PI).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(-PI, PI).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(-PI, PI).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(2.0, 5.0).toFloat(),
            Random.nextDouble(-PI, PI).toFloat(),
        ).toFloatArray()
    }
}

fun SandPathway.resolve(
    curve: Curve,
    amplitude: Float,
    t: Float
) = curve.value(t)
    .run { this + curve.normal(t) * amplitude * value(t) }

fun SandPathway.toPath(
    curve: Curve,
    amplitude: Float,
    steps: Int = 1000
): Path = Path().apply {
    val step = 1f / steps
    resolve(curve, amplitude, 0.0f)
        .also { offset -> moveTo(offset.x, offset.y) }
    for (idx in 1..steps) {
        (idx * step).let { t ->
            resolve(curve, amplitude, t)
                .also { offset -> lineTo(offset.x, offset.y) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SandPathwaySample() {
    MaterialTheme {
        val sandParticles: List<SandParticle> = List(1500) {
            SandParticle(
                t = Random.nextDouble(0.0, 1.0).toFloat(),
                velocity = Random.nextDouble(0.75, 1.25).toFloat(),
                sandPathway = SandPathway(SandPathway.randomizedFactors()),
                color = SandParticle.sandColor(),
            )
        }
        var period by remember { mutableStateOf(0f) }
        FrameEffect { time ->
            period = (0.03f * time).mod(1f)
        }
        val sandPathways = remember {
            mutableStateListOf(
                SandPathway(SandPathway.randomizedFactors()),
                SandPathway(SandPathway.randomizedFactors()),
                SandPathway(SandPathway.randomizedFactors()),
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
                .onClick {
                    sandPathways.clear()
                    sandPathways.addAll(
                        mutableStateListOf(
                            SandPathway(SandPathway.randomizedFactors()),
                            SandPathway(SandPathway.randomizedFactors()),
                            SandPathway(SandPathway.randomizedFactors()),
                        )
                    )
                },
        ) {
            items(sandPathways) {
                SandPathwayGraph(
                    sandPathway = it,
                    modifier = Modifier
                        .padding(20.dp)
                        .requiredHeight(50.dp)
                )
            }
            item {
                SandFlow(
                    sandPathways = sandPathways,
                    period = period,
                    modifier = Modifier
                        .padding(20.dp)
                        .requiredHeight(50.dp)
                )
            }
            item {
                SandParticles(
                    sandParticles = sandParticles,
                    period = period,
                    modifier = Modifier
                        .padding(20.dp)
                        .requiredHeight(20.dp)
                )
            }
        }
    }
}

@Composable
fun SandPathwayGraph(
    sandPathway: SandPathway,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawLine(
            color = Color.White,
            start = Offset(x = 0.0f, y = size.height * 0.5f),
            end = Offset(x = size.width, y = size.height * 0.5f),
        )
        drawLine(
            color = Color.Gray,
            start = Offset(x = 0.0f, y = 0.0f),
            end = Offset(x = size.width, y = 0.0f),
        )
        drawLine(
            color = Color.Gray,
            start = Offset(x = 0.0f, y = size.height),
            end = Offset(x = size.width, y = size.height),
        )
        sandPathway.toPath(
            curve = Line(
                start = Offset(x = 0.0f, y = size.height * 0.5f),
                end = Offset(x = size.width, y = size.height * 0.5f),
            ),
            amplitude = size.height * 0.5f,
        ).also { path ->
            drawPath(path, Color.White, style = Stroke())
        }
    }
}

@Composable
fun SandFlow(
    sandPathways: List<SandPathway>,
    period: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val curve = Line(
            start = Offset(x = 0.0f, y = size.height * 0.5f),
            end = Offset(x = size.width, y = size.height * 0.5f),
        )
        sandPathways.forEach { sandPathway ->
            sandPathway.toPath(
                curve = curve,
                amplitude = size.height * 0.5f,
            ).also { path ->
                drawPath(path, Color.LightGray, style = Stroke())
            }
            sandPathway.resolve(curve, size.height * 0.5f, period)
                .also {
                    drawCircle(
                        color = Color.White,
                        radius = size.maxDimension * 0.0025f,
                        center = it,
                    )
                }
        }
    }
}

@Composable
fun SandParticles(
    sandParticles: List<SandParticle>,
    period: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val curve = Line(
            start = Offset(x = 0.0f, y = size.height * 0.5f),
            end = Offset(x = size.width, y = size.height * 0.5f),
        )
        sandParticles.forEach { sandParticle ->
            sandParticle.sandPathway.resolve(
                curve,
                size.height * 0.5f,
                (sandParticle.t + sandParticle.velocity * period).mod(1.0f)
            )
                .also {
                    drawCircle(
                        color = sandParticle.color,
                        radius = size.maxDimension * 0.0025f,
                        center = it,
                    )
                }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sand Pathway Sample",
        state = rememberWindowState(width = 1280.dp, height = 720.dp),
        resizable = false,
    ) {
        SandPathwaySample()
    }
}
