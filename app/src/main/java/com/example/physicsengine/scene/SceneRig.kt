package com.example.physicsengine.scene

import kotlin.math.cos
import kotlin.math.max
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

    fun setRange(start: Int, end: Int, weight: Float) {
        for (index in start..end) {
            set(index, weight)
        }
    }

    fun setSoftFalloff(center: Int, radius: Int, peak: Float, base: Float = 0f) {
        for (index in max(0, center - radius)..(center + radius)) {
            val distance = kotlin.math.abs(index - center).toFloat()
            val falloff = (1f - (distance / (radius + 1f))).coerceIn(0f, 1f)
            val value = base + (peak - base) * falloff
            set(index, value)
        }
    }

    fun get(vertexIndex: Int): Float = weights[vertexIndex] ?: 0f

    fun clear() = weights.clear()

    fun normalize(vertexIndex: Int): Float = get(vertexIndex).coerceIn(0f, 1f)

    fun all(): Map<Int, Float> = weights.toMap()
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

    fun duration(): Float = if (keyframes.isEmpty()) 0f else keyframes.last().time
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

    fun setLoopPose(trackName: String, start: Vec2, end: Vec2, duration: Float) {
        val track = ensureTrack(trackName)
        track.addKeyframe(0f, start)
        track.addKeyframe(duration, end)
    }

    fun addMultiTrackPose(trackName: String, start: Vec2, mid: Vec2, end: Vec2, duration: Float) {
        val track = ensureTrack(trackName)
        track.addKeyframe(0f, start)
        track.addKeyframe(duration * 0.5f, mid)
        track.addKeyframe(duration, end)
    }

    fun sample(trackName: String, time: Float = currentTime): Vec2 {
        return tracks[trackName]?.sample(time) ?: Vec2(0f, 0f)
    }

    fun maxDuration(): Float = tracks.values.fold(0f) { acc, track -> max(acc, track.duration()) }

    fun update(dt: Float) {
        if (!playing) return
        currentTime += dt
        val maxTime = maxDuration()
        if (loop && maxTime > 0f && currentTime >= maxTime) {
            currentTime = 0f
        }
    }

    fun play() {
        playing = true
    }

    fun stop() {
        playing = false
    }

    fun reset() {
        currentTime = 0f
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

    fun applyBounds(width: Float, height: Float) {
        if (position.x < radius) {
            position.x = radius
            velocity.x *= -0.8f
        }
        if (position.x > width - radius) {
            position.x = width - radius
            velocity.x *= -0.8f
        }
        if (position.y < radius) {
            position.y = radius
            velocity.y *= -0.8f
        }
        if (position.y > height - radius) {
            position.y = height - radius
            velocity.y *= -0.8f
        }
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

    fun paintedWeight(index: Int): Float = when (bindingMode) {
        BindingMode.Full -> 1f
        BindingMode.Partial -> weightPaint.get(index).coerceIn(0f, 1f)
        BindingMode.PhysicsDriven -> 1f
    }

    fun finalSurface(animationPose: Vec2, physicsPose: Vec2): List<Vec2> {
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()

        return surfaceMesh.mapIndexed { index, vertex ->
            val weight = paintedWeight(index)
            val blended = blend(animationPose, physicsPose, weight)
            val px = (vertex.x * c - vertex.y * s) + blended.x + transform.x
            val py = (vertex.x * s + vertex.y * c) + blended.y + transform.y
            Vec2(px, py)
        }
    }

    fun finalPoint(vertex: Vec2, index: Int, animationPose: Vec2, physicsPose: Vec2): Vec2 {
        val weight = paintedWeight(index)
        val blended = blend(animationPose, physicsPose, weight)
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()
        val px = (vertex.x * c - vertex.y * s) + blended.x + transform.x
        val py = (vertex.x * s + vertex.y * c) + blended.y + transform.y
        return Vec2(px, py)
    }
}

class MeshRigScene(
    private val animationTimeline: AnimationTimeline,
    private val proxy: PhysicsProxy,
    private val meshBinding: MeshBinding
) {
    fun update(dt: Float) {
        proxy.step(dt, 900f)
        proxy.applyBounds(900f, 1200f)
        animationTimeline.update(dt)
    }

    fun finalSurface(): List<Vec2> {
        val animationPose = animationTimeline.sample("position")
        val physicsPose = proxy.position
        return meshBinding.finalSurface(animationPose, physicsPose)
    }

    fun setPlaying(enabled: Boolean) {
        animationTimeline.playing = enabled
    }

    fun setLoopPose(start: Vec2, end: Vec2, duration: Float) {
        animationTimeline.setLoopPose("position", start, end, duration)
    }

    fun setTime(time: Float) {
        animationTimeline.currentTime = time
    }

    fun setSecondaryMotion(start: Vec2, mid: Vec2, end: Vec2, duration: Float) {
        animationTimeline.addMultiTrackPose("secondary", start, mid, end, duration)
    }
}

class SceneRigBuilder {
    fun createScene(): MeshRigScene {
        val timeline = AnimationTimeline().apply {
            setLoopPose("position", Vec2(110f, 180f), Vec2(260f, 150f), 2f)
            addMultiTrackPose("secondary", Vec2(0f, 0f), Vec2(12f, -18f), Vec2(2f, 6f), 1.5f)
            play()
        }

        val proxy = PhysicsProxy(
            position = Vec2(150f, 190f),
            velocity = Vec2(35f, 18f),
            radius = 28f
        )

        val points = listOf(
            Vec2(-38f, -26f),
            Vec2(18f, -44f),
            Vec2(52f, -18f),
            Vec2(54f, 16f),
            Vec2(26f, 38f),
            Vec2(-10f, 46f),
            Vec2(-42f, 30f),
            Vec2(-52f, 4f)
        )

        val weightPaint = WeightPaint().apply {
            setRange(0, 1, 0.35f)
            setRange(2, 3, 0.6f)
            setRange(4, 5, 0.82f)
            setRange(6, 7, 0.9f)
            setSoftFalloff(3, 2, 0.75f)
        }

        val meshBinding = MeshBinding(
            surfaceMesh = points,
            collisionMesh = points,
            weightPaint = weightPaint,
            bindingMode = BindingMode.Partial,
            transform = Vec2(0f, 0f),
            rotation = 0.2f
        )

        return MeshRigScene(
            animationTimeline = timeline,
            proxy = proxy,
            meshBinding = meshBinding
        )
    }
}
