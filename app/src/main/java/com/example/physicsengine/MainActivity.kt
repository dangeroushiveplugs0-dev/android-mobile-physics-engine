package com.example.physicsengine

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.physicsengine.scene.SceneRigBuilder
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SceneRigDemo()
            }
        }
    }
}

@Composable
private fun SceneRigDemo() {
    val sceneRig = remember { SceneRigBuilder().createScene() }
    var worldSize by remember { mutableStateOf(IntSize(900, 1200)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            sceneRig.update(0.016f)
            worldSize = IntSize(900, 1200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09121E))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101A2B), RoundedCornerShape(22.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val points = sceneRig.finalSurface()
                if (points.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                        close()
                    }
                    drawPath(path, color = Color(0xFF72D5FF))
                    drawPath(path, color = Color.White.copy(alpha = 0.20f), style = Stroke(width = 1.5f))
                }

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
        }
    }
}
