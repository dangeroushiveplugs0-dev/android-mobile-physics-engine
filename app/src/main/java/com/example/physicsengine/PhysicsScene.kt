package com.example.physicsengine

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PhysicsScene {
    var width: Float = 900f
    var height: Float = 1200f
    var gravity: Float = 1100f

    val bodies = mutableListOf<MeshBody>()

    private var dragBody: MeshBody? = null

    init {
        reset()
    }

    fun resize(newWidth: Float, newHeight: Float) {
        width = newWidth
        height = newHeight
        bodies.forEach { body ->
            body.position.x = body.position.x.coerceIn(body.radius, width - body.radius)
            body.position.y = body.position.y.coerceIn(body.radius, height - body.radius)
        }
    }

    fun reset() {
        bodies.clear()

        val floor = MeshBody(
            id = "floor",
            position = Vec2(width * 0.5f, height - 22f),
            localVertices = listOf(
                Vec2(-240f, -18f),
                Vec2(240f, -18f),
                Vec2(240f, 18f),
                Vec2(-240f, 18f)
            ),
            velocity = Vec2(0f, 0f),
            rotation = 0f,
            scale = Vec2(1f, 1f),
            color = Color(0xFF1F2A3C),
            mass = 1000000f,
            isStatic = true,
            radius = 240f
        )
        bodies.add(floor)

        bodies.add(
            MeshBody(
                id = "box_1",
                position = Vec2(180f, 120f),
                localVertices = listOf(
                    Vec2(-36f, -20f),
                    Vec2(36f, -20f),
                    Vec2(36f, 20f),
                    Vec2(-36f, 20f)
                ),
                velocity = Vec2(160f, 30f),
                color = Color(0xFF72D5FF),
                mass = 2.2f,
                radius = 42f
            )
        )

        bodies.add(
            MeshBody(
                id = "diamond_1",
                position = Vec2(520f, 170f),
                localVertices = listOf(
                    Vec2(0f, -46f),
                    Vec2(46f, 0f),
                    Vec2(0f, 46f),
                    Vec2(-46f, 0f)
                ),
                velocity = Vec2(-140f, -10f),
                color = Color(0xFF8CF7B8),
                mass = 2.4f,
                radius = 56f
            )
        )

        bodies.add(
            MeshBody(
                id = "hex_1",
                position = Vec2(320f, 80f),
                localVertices = createHexagon(28f),
                velocity = Vec2(90f, 60f),
                color = Color(0xFFFFC857),
                mass = 1.8f,
                radius = 32f
            )
        )
    }

    fun addMeshBody() {
        val sizeBase = 20f + (Math.random() * 22f).toFloat()
        val base = if ((Math.random() * 3f).toInt() % 3 == 0) {
            createDiamond(sizeBase * 2.2f, sizeBase * 2.2f)
        } else if ((Math.random() * 3f).toInt() % 3 == 1) {
            createBox(sizeBase * 2.2f, sizeBase * 1.8f)
        } else {
            createHexagon(sizeBase)
        }

        bodies.add(
            MeshBody(
                id = "dynamic_${bodies.size}",
                position = Vec2(
                    100f + (Math.random() * (width - 200f)).toFloat(),
                    80f + (Math.random() * (height * 0.28f)).toFloat()
                ),
                localVertices = base,
                velocity = Vec2(
                    (Math.random() * 180f - 90f).toFloat(),
                    (Math.random() * 90f - 30f).toFloat()
                ),
                color = palette[bodies.size % palette.size],
                mass = 1.6f + (Math.random() * 1.6f).toFloat(),
                radius = (base.maxOf { v -> v.x * v.x + v.y * v.y }.toDouble().sqrt() + 8f).toFloat()
            )
        )
    }

    fun tryDrag(px: Float, py: Float): Boolean {
        val target = bodies.asReversed().firstOrNull { body -> body.contains(px, py) }
        target?.let {
            dragBody = it
            it.dragging = true
            it.position = Vec2(px, py)
            it.velocity = Vec2(0f, 0f)
        }
        return target != null
    }

    fun dragTo(px: Float, py: Float) {
        dragBody?.let { body ->
            body.position = Vec2(px, py)
            body.velocity = Vec2(0f, 0f)
        }
    }

    fun releaseDrag() {
        dragBody?.dragging = false
        dragBody = null
    }

    fun step(dt: Float) {
        val fixedDt = 1f / 120f
        var accumulator = dt
        while (accumulator > 0f) {
            val step = kotlin.math.min(fixedDt, accumulator)
            integrate(step)
            accumulator -= step
        }
    }

    private fun integrate(dt: Float) {
        for (body in bodies) {
            if (body.isStatic || body.dragging) continue
            body.velocity.y += gravity * dt
            body.position.x += body.velocity.x * dt
            body.position.y += body.velocity.y * dt
            resolveWorldBounds(body)
        }
        resolvePairwiseCollisions()
    }

    private fun resolveWorldBounds(body: MeshBody) {
        val r = body.radius
        if (body.position.x < r) {
            body.position.x = r
            body.velocity.x = abs(body.velocity.x) * 0.8f
        }
        if (body.position.x > width - r) {
            body.position.x = width - r
            body.velocity.x = -abs(body.velocity.x) * 0.8f
        }
        if (body.position.y < r) {
            body.position.y = r
            body.velocity.y = abs(body.velocity.y) * 0.8f
        }
        if (body.position.y > height - r) {
            body.position.y = height - r
            body.velocity.y = -abs(body.velocity.y) * 0.8f
            body.velocity.x *= 0.995f
        }
    }

    private fun resolvePairwiseCollisions() {
        for (i in bodies.indices) {
            val a = bodies[i]
            if (a.isStatic || a.dragging) continue
            for (j in i + 1 until bodies.size) {
                val b = bodies[j]
                if (b.isStatic || b.dragging) continue

                val dx = b.position.x - a.position.x
                val dy = b.position.y - a.position.y
                val minimum = a.radius + b.radius
                val distanceSq = dx * dx + dy * dy
                if (distanceSq >= minimum * minimum) continue

                val distance = sqrt(distanceSq.toDouble()).toFloat().coerceAtLeast(0.0001f)
                val nx = if (distance > 0f) dx / distance else 1f
                val ny = if (distance > 0f) dy / distance else 0f
                val overlap = minimum - distance

                val invMassA = if (a.mass <= 0f) 0f else 1f / a.mass
                val invMassB = if (b.mass <= 0f) 0f else 1f / b.mass
                val totalInv = invMassA + invMassB
                if (totalInv <= 0f) continue

                val correction = overlap / totalInv
                a.position.x -= nx * correction * invMassA
                a.position.y -= ny * correction * invMassA
                b.position.x += nx * correction * invMassB
                b.position.y += ny * correction * invMassB

                val rvx = b.velocity.x - a.velocity.x
                val rvy = b.velocity.y - a.velocity.y
                val velAlongNormal = rvx * nx + rvy * ny
                if (velAlongNormal > 0f) continue

                val restitution = 0.8f
                val impulse = (-(1f + restitution) * velAlongNormal) / totalInv
                val ix = impulse * nx
                val iy = impulse * ny
                a.velocity.x -= ix * invMassA
                a.velocity.y -= iy * invMassA
                b.velocity.x += ix * invMassB
                b.velocity.y += iy * invMassB
            }
        }
    }

    private fun createHexagon(radius: Float): List<Vec2> {
        val points = mutableListOf<Vec2>()
        for (i in 0 until 6) {
            val angle = (Math.PI * 2.0 * i / 6.0).toFloat()
            points.add(Vec2(cos(angle.toDouble()).toFloat() * radius, sin(angle.toDouble()).toFloat() * radius))
        }
        return points
    }

    private fun createBox(width: Float, height: Float): List<Vec2> = listOf(
        Vec2(-width / 2f, -height / 2f),
        Vec2(width / 2f, -height / 2f),
        Vec2(width / 2f, height / 2f),
        Vec2(-width / 2f, height / 2f)
    )

    private fun createDiamond(width: Float, height: Float): List<Vec2> = listOf(
        Vec2(0f, -height / 2f),
        Vec2(width / 2f, 0f),
        Vec2(0f, height / 2f),
        Vec2(-width / 2f, 0f)
    )

    data class Vec2(var x: Float, var y: Float)

    data class MeshBody(
        val id: String,
        var position: Vec2,
        val localVertices: List<Vec2>,
        var velocity: Vec2,
        var rotation: Float = 0f,
        var scale: Vec2 = Vec2(1f, 1f),
        val color: Color,
        val mass: Float = 1f,
        val radius: Float = 32f,
        val isStatic: Boolean = false,
        var dragging: Boolean = false
    ) {
        fun transformedVertices(): List<Vec2> {
            val c = cos(rotation.toDouble()).toFloat()
            val s = sin(rotation.toDouble()).toFloat()
            return localVertices.map { v ->
                val x = (v.x * scale.x * c - v.y * scale.y * s) + position.x
                val y = (v.x * scale.x * s + v.y * scale.y * c) + position.y
                Vec2(x, y)
            }
        }

        fun contains(px: Float, py: Float): Boolean {
            val verts = transformedVertices()
            if (verts.isEmpty()) return false
            val dx = px - position.x
            val dy = py - position.y
            if (dx * dx + dy * dy > radius * radius) return false
            var inside = false
            var j = verts.size - 1
            for (i in verts.indices) {
                val xi = verts[i].x
                val yi = verts[i].y
                val xj = verts[j].x
                val yj = verts[j].y
                val intersects = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi + 1e-6f) + xi)
                if (intersects) {
                    inside = !inside
                }
                j = i
            }
            return inside
        }
    }

    companion object {
        private val palette = listOf(
            Color(0xFF72D5FF),
            Color(0xFF8CF7B8),
            Color(0xFFFFB86C),
            Color(0xFFD69CFF),
            Color(0xFFFF7E8A),
            Color(0xFF98EE90)
        )
    }
}
