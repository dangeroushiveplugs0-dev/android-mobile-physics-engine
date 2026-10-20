package com.example.physicsengine

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PhysicsEngineApp()
            }
        }
    }
}

@Composable
private fun PhysicsEngineApp(
    viewModel: PhysicsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1020))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = Color(0xFF101A2B),
                    shape = RoundedCornerShape(20.dp)
                )
                .pointerInput(state.worldSize.width, state.worldSize.height) {
                    detectDragGestures { change, dragAmount ->
                        val p = change.position
                        val worldX = p.x
                        val worldY = p.y
                        viewModel.dragBody(worldX, worldY)
                        if (change.pressed) {
                            change.consume()
                        }
                    }
                }
                .onSizeChanged { size: IntSize ->
                    viewModel.updateWorldSize(size.width.toFloat(), size.height.toFloat())
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGrid()
                state.bodies.forEach { body ->
                    drawCircle(
                        color = body.color,
                        radius = body.radius,
                        center = Offset(body.x, body.y)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = body.radius * 0.3f,
                        center = Offset(body.x - body.radius * 0.35f, body.y - body.radius * 0.35f),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.spawnBall() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Ball")
            }

            Button(
                onClick = { viewModel.resetScene() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111B2E), shape = RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("Gravity: ${state.gravity.roundToInt()} px/s²")
            Slider(
                value = state.gravity,
                onValueChange = { viewModel.setGravity(it) },
                valueRange = 300f..2200f,
                steps = 18
            )
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            viewModel.updatePhysics(0.016f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    for (x in 0 until size.width.toInt() step 32) {
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(x.toFloat(), 0f),
            end = Offset(x.toFloat(), size.height),
            strokeWidth = 1f
        )
    }
    for (y in 0 until size.height.toInt() step 32) {
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, y.toFloat()),
            end = Offset(size.width, y.toFloat()),
            strokeWidth = 1f
        )
    }
}

private data class PhysicsBodyUi(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color
)

private data class PhysicsUiState(
    val bodies: List<PhysicsBodyUi>,
    val gravity: Float,
    val worldSize: IntSize = IntSize(0, 0)
)

class PhysicsViewModel : androidx.lifecycle.ViewModel() {
    private val world = PhysicsWorld(
        width = 800f,
        height = 1200f,
        gravity = Vec2(0f, 1100f)
    )

    private val _state = androidx.compose.runtime.mutableStateOf(
        PhysicsUiState(
            bodies = emptyList(),
            gravity = 1100f,
            worldSize = IntSize(800, 1200)
        )
    )
    val state: kotlinx.coroutines.flow.StateFlow<PhysicsUiState>
        get() = _stateFlow

    private val _stateFlow = kotlinx.coroutines.flow.MutableStateFlow(
        PhysicsUiState(
            bodies = emptyList(),
            gravity = 1100f,
            worldSize = IntSize(800, 1200)
        )
    )

    init {
        resetScene()
    }

    fun setGravity(value: Float) {
        world.gravity.y = value
        _stateFlow.value = _stateFlow.value.copy(gravity = value)
    }

    fun updateWorldSize(width: Float, height: Float) {
        world.width = width
        world.height = height
        _stateFlow.value = _stateFlow.value.copy(worldSize = IntSize(width.roundToInt(), height.roundToInt()))
    }

    fun dragBody(worldX: Float, worldY: Float) {
        world.dragBody(worldX, worldY)
        syncUi()
    }

    fun spawnBall() {
        world.spawnBall()
        syncUi()
    }

    fun resetScene() {
        world.resetScene()
        syncUi()
    }

    fun updatePhysics(dt: Float) {
        world.update(dt)
        syncUi()
    }

    private fun syncUi() {
        val uiBodies = world.bodies.map { body ->
            PhysicsBodyUi(
                x = body.x,
                y = body.y,
                radius = body.radius,
                color = Color(body.color)
            )
        }
        _stateFlow.value = PhysicsUiState(
            bodies = uiBodies,
            gravity = world.gravity.y,
            worldSize = IntSize(world.width.roundToInt(), world.height.roundToInt())
        )
    }
}

class PhysicsWorld(
    var width: Float,
    var height: Float,
    var gravity: Vec2 = Vec2(0f, 900f)
) {
    val bodies = mutableListOf<PhysicsBody>()
    private var draggedBody: PhysicsBody? = null

    init {
        resetScene()
    }

    fun resetScene() {
        bodies.clear()
        repeat(10) { index ->
            val radius = 14f + (index % 4) * 7f
            val x = 70f + (index * 73f % (width - 140f))
            val y = 60f + (index * 41f % (height * 0.38f))
            bodies.add(
                PhysicsBody(
                    x = x,
                    y = y,
                    vx = ((index % 2) * 2f - 1f) * 80f,
                    vy = ((index % 3) * 1.5f - 1.5f) * 20f,
                    radius = radius,
                    mass = radius * radius * 0.08f,
                    color = Color.hsl(220f + index * 18f, 0.75f, 0.62f)
                )
            )
        }
    }

    fun spawnBall() {
        val radius = 16f + (Math.random() * 18f).toFloat()
        val x = 80f + (Math.random() * (width - 160f)).toFloat()
        val y = 50f + (Math.random() * (height * 0.25f)).toFloat()
        bodies.add(
            PhysicsBody(
                x = x,
                y = y,
                vx = (Math.random() * 220f - 110f).toFloat(),
                vy = (Math.random() * 90f - 45f).toFloat(),
                radius = radius,
                mass = radius * radius * 0.08f,
                color = Color.hsl((Math.random() * 200f + 120f).toFloat(), 0.72f, 0.66f)
            )
        )
    }

    fun dragBody(worldX: Float, worldY: Float) {
        val hit = bodies.asReversed().firstOrNull { b ->
            val dx = worldX - b.x
            val dy = worldY - b.y
            dx * dx + dy * dy <= b.radius * b.radius
        }

        if (hit != null) {
            draggedBody = hit
            hit.x = worldX
            hit.y = worldY
            hit.vx = 0f
            hit.vy = 0f
        }
    }

    fun update(dt: Float) {
        val fixedDt = 1f / 120f
        var accumulator = dt

        while (accumulator > 0f) {
            val step = minOf(fixedDt, accumulator)
            stepSimulation(step)
            accumulator -= step
        }
    }

    private fun stepSimulation(dt: Float) {
        for (body in bodies) {
            if (body == draggedBody) continue
            body.vy += gravity.y * dt
            body.vx += gravity.x * dt
            body.x += body.vx * dt
            body.y += body.vy * dt

            body.vx *= 0.999f
            body.vy *= 0.999f

            resolveWall(body)
        }

        resolveBodyCollisions()
    }

    private fun resolveWall(body: PhysicsBody) {
        val r = body.radius
        if (body.x < r) {
            body.x = r
            body.vx = kotlin.math.abs(body.vx) * 0.82f
        }
        if (body.x > width - r) {
            body.x = width - r
            body.vx = -kotlin.math.abs(body.vx) * 0.82f
        }
        if (body.y < r) {
            body.y = r
            body.vy = kotlin.math.abs(body.vy) * 0.82f
        }
        if (body.y > height - r) {
            body.y = height - r
            body.vy = -kotlin.math.abs(body.vy) * 0.82f
            body.vx *= 0.995f
        }
    }

    private fun resolveBodyCollisions() {
        val count = bodies.size
        for (i in 0 until count) {
            val a = bodies[i]
            for (j in i + 1 until count) {
                val b = bodies[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val minDist = a.radius + b.radius
                val dist2 = dx * dx + dy * dy

                if (dist2 >= minDist * minDist) continue

                val dist = kotlin.math.sqrt(dist2.toDouble()).toFloat().coerceAtLeast(0.0001f)
                val nx = dx / dist
                val ny = dy / dist
                val overlap = minDist - dist

                val invMassA = if (a.mass == 0f) 0f else 1f / a.mass
                val invMassB = if (b.mass == 0f) 0f else 1f / b.mass
                val totalInvMass = invMassA + invMassB
                if (totalInvMass == 0f) continue

                val correction = overlap / totalInvMass
                a.x -= nx * correction * invMassA
                a.y -= ny * correction * invMassA
                b.x += nx * correction * invMassB
                b.y += ny * correction * invMassB

                val rvx = b.vx - a.vx
                val rvy = b.vy - a.vy
                val velAlongNormal = rvx * nx + rvy * ny
                if (velAlongNormal > 0f) continue

                val restitution = 0.82f
                val impulse = (-(1f + restitution) * velAlongNormal) / totalInvMass
                val ix = impulse * nx
                val iy = impulse * ny

                a.vx -= ix * invMassA
                a.vy -= iy * invMassA
                b.vx += ix * invMassB
                b.vy += iy * invMassB
            }
        }
    }
}

private data class PhysicsBody(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val mass: Float,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White
)

private data class Vec2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
}

private fun Color.Companion.hsl(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val r: Float
    val g: Float
    val b: Float

    return when {
        hue < 60f -> { r = c; g = x; b = 0f }
        hue < 120f -> { r = x; g = c; b = 0f }
        hue < 180f -> { r = 0f; g = c; b = x }
        hue < 240f -> { r = 0f; g = x; b = c }
        hue < 300f -> { r = x; g = 0f; b = c }
        else -> { r = c; g = 0f; b = x }
    }.let { androidx.compose.ui.graphics.Color(r + m, g + m, b + m) }
}
