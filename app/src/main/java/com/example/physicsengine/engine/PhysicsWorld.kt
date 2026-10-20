package com.example.physicsengine.engine

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PhysicsWorld(
    var width: Float = 800f,
    var height: Float = 1200f,
    var gravity: Vec2 = Vec2(0f, 1200f)
) {
    val bodies = mutableListOf<PhysicsBody>()
    private var draggedBody: PhysicsBody? = null

    fun resetScene() {
        bodies.clear()
        repeat(12) { index ->
            val radius = 16f + (index % 5) * 7f
            val x = 80f + ((index * 67) % max(1f, width - 160f))
            val y = 80f + ((index * 41) % max(1f, height * 0.28f))
            bodies += PhysicsBody(
                id = "ball_$index",
                position = Vec2(x, y),
                velocity = Vec2((((index % 2) * 2f) - 1f) * 120f, (((index % 3) * 2f) - 1f) * 40f),
                radius = radius,
                mass = radius * radius * 0.12f,
                color = samplePalette(index),
                bodyType = BodyType.Dynamic
            )
        }
    }

    fun createBall(
        x: Float,
        y: Float,
        radius: Float = 18f,
        bodyType: BodyType = BodyType.Dynamic,
        color: Color = samplePalette((bodies.size + 1) % 12)
    ): PhysicsBody {
        val body = PhysicsBody(
            id = "ball_${bodies.size}",
            position = Vec2(x, y),
            velocity = Vec2(0f, 0f),
            radius = radius,
            mass = radius * radius * 0.12f,
            color = color,
            bodyType = bodyType
        )
        bodies += body
        return body
    }

    fun addBody(body: PhysicsBody): PhysicsBody {
        bodies += body
        return body
    }

    fun dragBody(worldX: Float, worldY: Float): PhysicsBody? {
        val hit = bodies.asReversed().firstOrNull { body ->
            val dx = worldX - body.position.x
            val dy = worldY - body.position.y
            dx * dx + dy * dy <= body.radius * body.radius
        }

        if (hit != null) {
            draggedBody = hit
            hit.position = Vec2(worldX, worldY)
            hit.velocity = Vec2(0f, 0f)
            hit.bodyType = BodyType.Kinematic
        }
        return hit
    }

    fun releaseDrag() {
        draggedBody?.bodyType = BodyType.Dynamic
        draggedBody = null
    }

    fun update(dt: Float) {
        val fixedDt = 1f / 120f
        var remaining = dt

        while (remaining > 0f) {
            val step = min(fixedDt, remaining)
            stepSimulation(step)
            remaining -= step
        }
    }

    private fun stepSimulation(dt: Float) {
        for (body in bodies) {
            if (body == draggedBody) continue
            if (body.bodyType == BodyType.Static) continue

            body.velocity += gravity * dt
            body.position += body.velocity * dt
            resolveWorldBounds(body)
        }

        resolveCollisions()
    }

    private fun resolveWorldBounds(body: PhysicsBody) {
        val r = body.radius
        if (body.position.x < r) {
            body.position.x = r
            body.velocity.x = abs(body.velocity.x) * 0.78f
        } else if (body.position.x > width - r) {
            body.position.x = width - r
            body.velocity.x = -abs(body.velocity.x) * 0.78f
        }

        if (body.position.y < r) {
            body.position.y = r
            body.velocity.y = abs(body.velocity.y) * 0.78f
        } else if (body.position.y > height - r) {
            body.position.y = height - r
            body.velocity.y = -abs(body.velocity.y) * 0.78f
            body.velocity.x *= 0.995f
        }
    }

    private fun resolveCollisions() {
        for (i in 0 until bodies.size) {
            val a = bodies[i]
            if (a.bodyType == BodyType.Static) continue

            for (j in i + 1 until bodies.size) {
                val b = bodies[j]
                if (b.bodyType == BodyType.Static) continue

                val dx = b.position.x - a.position.x
                val dy = b.position.y - a.position.y
                val minDist = a.radius + b.radius
                val distSq = dx * dx + dy * dy
                if (distSq >= minDist * minDist) continue

                val dist = sqrt(distSq.toDouble()).toFloat().coerceAtLeast(0.0001f)
                val nx = dx / dist
                val ny = dy / dist

                val overlap = minDist - dist
                val invMassA = if (a.mass <= 0f) 0f else 1f / a.mass
                val invMassB = if (b.mass <= 0f) 0f else 1f / b.mass
                val totalInvMass = invMassA + invMassB
                if (totalInvMass == 0f) continue

                val correction = overlap / totalInvMass
                a.position -= Vec2(nx * correction * invMassA, ny * correction * invMassA)
                b.position += Vec2(nx * correction * invMassB, ny * correction * invMassB)

                val relativeVel = b.velocity - a.velocity
                val velAlongNormal = relativeVel.x * nx + relativeVel.y * ny
                if (velAlongNormal > 0f) continue

                val restitution = 0.82f
                val impulseMagnitude = (-(1f + restitution) * velAlongNormal) / totalInvMass
                val impulse = Vec2(nx, ny) * impulseMagnitude
                a.velocity -= impulse * invMassA
                b.velocity += impulse * invMassB
            }
        }
    }

    companion object {
        fun samplePalette(index: Int): Color {
            val palette = listOf(
                Color(0xFF72D5FF),
                Color(0xFF8CF7B8),
                Color(0xFF7C8CFF),
                Color(0xFFFFB86C),
                Color(0xFFFF7E8A),
                Color(0xFFB4FF7A),
                Color(0xFFD69CFF),
                Color(0xFF7AE7FF),
                Color(0xFFFFD166),
                Color(0xFF9DFFB0),
                Color(0xFF90CAF9),
                Color(0xFFCE93D8)
            )
            return palette[index % palette.size]
        }
    }
}

private fun abs(value: Float): Float = if (value < 0f) -value else value
