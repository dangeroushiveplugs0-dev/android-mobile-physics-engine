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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PhysicsToolkitDemo()
            }
        }
    }
}

@Composable
private fun PhysicsToolkitDemo() {
    val scene = remember { PhysicsScene() }
    var gravity by remember { mutableFloatStateOf(scene.gravity) }
    var dragMode by remember { mutableStateOf(false) }
    var worldSize by remember { mutableStateOf(IntSize(900, 1200)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            scene.step(0.016f)
            gravity = scene.gravity
            worldSize = IntSize(scene.width.roundToInt(), scene.height.roundToInt())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09121E))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF101A2B), RoundedCornerShape(22.dp))
                .pointerInput(worldSize.width, worldSize.height) {
                    detectDragGestures(
                        onDragStart = { change ->
                            dragMode = scene.tryDrag(change.position.x, change.position.y)
                            change.consume()
                        },
                        onDrag = { change, _ ->
                            if (dragMode) {
                                scene.dragTo(change.position.x, change.position.y)
                            }
                        },
                        onDragEnd = {
                            scene.releaseDrag()
                            dragMode = false
                        },
                        onDragCancel = {
                            scene.releaseDrag()
                            dragMode = false
                        }
                    )
                }
                .onSizeChanged { size ->
                    scene.resize(size.width.toFloat(), size.height.toFloat())
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGrid()
                scene.bodies.forEach { body ->
                    val poly = body.transformedVertices()
                    if (poly.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(poly.first().x, poly.first().y)
                            for (i in 1 until poly.size) lineTo(poly[i].x, poly[i].y)
                            close()
                        }
                        drawPath(path, color = body.color)
                        drawPath(path, color = Color.White.copy(alpha = 0.18f), style = Stroke(width = 1.2f))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { scene.addMeshBody() }, modifier = Modifier.weight(1f)) {
                Text("Add Mesh")
            }
            Button(onClick = { scene.reset() }, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111D2D), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text("Gravity: ${gravity.roundToInt()} px/s²")
            Slider(
                value = gravity,
                onValueChange = {
                    gravity = it
                    scene.gravity = it
                },
                valueRange = 300f..2200f,
                steps = 18
            )
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
