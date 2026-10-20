package com.example.physicsengine.scene

import kotlin.math.cos
import kotlin.math.sin

data class Vec2(
    var x: Float,
    var y: Float
) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(s: Float): Vec2 = Vec2(x * s, y * s)
    operator fun div(s: Float): Vec2 = Vec2(x / s, y / s)
}

enum class BindingMode {
    Full,
    Partial,
    PhysicsDriven
}

class WeightPaint {
    private val weights = linkedMapOf<Int, Float>()

    fun set(vertexIndex: Int, weight: Float) {
        weights[vertexIndex] = weight.coerceIn(0f, 1f)
    }

    fun get(vertexIndex: Int): Float = weights[vertexIndex] ?: 0f

    fun clear() = weights.clear()

    fun normalize(vertexIndex: Int): Float = get(vertexIndex).coerceIn(0f, 1f)
}

class AnimationTrack {
    data class Keyframe(val time: Float, val value: Vec2)

    private val keyframes = mutableListOf<Keyframe>()

    fun addKeyframe(time: Float, value: Vec2) {
        keyframes.add(Keyframe(time, value))
        keyframes.sortBy { it.time }
    }

    fun sample(time: Float): Vec2 {
        if (keyframes.isEmpty()) return Vec2(0f, 0f)
        if (keyframes.size == 1) return keyframes.first().value

        var previous = keyframes.first()
        var next = keyframes.first()

        for (frame in keyframes) {
            if (frame.time <= time) previous = frame
            if (frame.time >= time) {
                next = frame
                break
            }
        }

        if (previous.time == next.time) return previous.value

        val t = ((time - previous.time) / (next.time - previous.time)).coerceIn(0f, 1f)
        val smooth = t * t * (3f - 2f * t)

        val dx = next.value.x - previous.value.x
        val dy = next.value.y - previous.value.y

        return Vec2(
            previous.value.x + dx * smooth,
            previous.value.y + dy * smooth
        )
    }
}

class AnimationTimeline {
    private val tracks = linkedMapOf<String, AnimationTrack>()
    var currentTime: Float = 0f
    var playing: Boolean = false
    var loop: Boolean = true

    fun ensureTrack(name: String): AnimationTrack {
        return tracks.getOrPut(name) { AnimationTrack() }
    }

    fun addKeyframe(trackName: String, time: Float, value: Vec2) {
        ensureTrack(trackName).addKeyframe(time, value)
    }

    fun sample(trackName: String): Vec2 {
        return tracks[trackName]?.sample(currentTime) ?: Vec2(0f, 0f)
    }

    fun update(dt: Float) {
        if (!playing) return
        currentTime += dt
    }
}

class PhysicsProxy(
    var position: Vec2,
    var velocity: Vec2 = Vec2(0f, 0f),
    var radius: Float = 24f
) {
    fun step(dt: Float, gravity: Float) {
        velocity.y += gravity * dt
        position.x += velocity.x * dt
        position.y += velocity.y * dt
    }
}

class MeshBinding(
    val surfaceMesh: List<Vec2>,
    val collisionMesh: List<Vec2>,
    val weightPaint: WeightPaint,
    var transform: Vec2 = Vec2(0f, 0f),
    var rotation: Float = 0f,
    val bindingMode: BindingMode = BindingMode.Partial
) {
    fun blend(animationPose: Vec2, physicsPose: Vec2, weight: Float): Vec2 {
        val x = animationPose.x * (1f - weight) + physicsPose.x * weight
        val y = animationPose.y * (1f - weight) + physicsPose.y * weight
        return Vec2(x, y)
    }

    fun finalSurface(animationPose: Vec2, physicsPose: Vec2): List<Vec2> {
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()

        return surfaceMesh.mapIndexed { index, vertex ->
            val w = when (bindingMode) {
                BindingMode.Full -> 1f
                BindingMode.Partial -> weightPaint.get(index).coerceIn(0f, 1f)
                BindingMode.PhysicsDriven -> 1f
            }

            val blended = blend(animationPose, physicsPose, w)

            val px = (vertex.x * c - vertex.y * s) + blended.x + transform.x
            val py = (vertex.x * s + vertex.y * c) + blended.y + transform.y

            Vec2(px, py)
        }
    }
}

class MeshRigScene(
    private val animationTimeline: AnimationTimeline,
    private val proxy: PhysicsProxy,
    private val meshBinding: MeshBinding
) {
    fun update(dt: Float) {
        proxy.step(dt, 900f)
        animationTimeline.update(dt)
    }

    fun finalSurface(): List<Vec2> {
        val animationPose = animationTimeline.sample("position")
        val physicsPose = proxy.position
        return meshBinding.finalSurface(animationPose, physicsPose)
    }
}
