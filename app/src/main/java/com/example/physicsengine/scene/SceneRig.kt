package com.example.physicsengine.scene

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Vec3(
    var x: Float,
    var y: Float,
    var z: Float
) {
    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(s: Float): Vec3 = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float): Vec3 = Vec3(x / s, y / s, z / s)
}

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
            val distance = abs(index - center).toFloat()
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
    data class Keyframe(val time: Float, val value: Vec3)

    private val keyframes = mutableListOf<Keyframe>()

    fun addKeyframe(time: Float, value: Vec3) {
        keyframes.add(Keyframe(time, value))
        keyframes.sortBy { it.time }
    }

    fun sample(time: Float): Vec3 {
        if (keyframes.isEmpty()) return Vec3(0f, 0f, 0f)
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

        return Vec3(
            previous.value.x + (next.value.x - previous.value.x) * smooth,
            previous.value.y + (next.value.y - previous.value.y) * smooth,
            previous.value.z + (next.value.z - previous.value.z) * smooth
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

    fun addKeyframe(trackName: String, time: Float, value: Vec3) {
        ensureTrack(trackName).addKeyframe(time, value)
    }

    fun setLoopPose(trackName: String, start: Vec3, end: Vec3, duration: Float) {
        val track = ensureTrack(trackName)
        track.addKeyframe(0f, start)
        track.addKeyframe(duration, end)
    }

    fun addMultiTrackPose(trackName: String, start: Vec3, mid: Vec3, end: Vec3, duration: Float) {
        val track = ensureTrack(trackName)
        track.addKeyframe(0f, start)
        track.addKeyframe(duration * 0.5f, mid)
        track.addKeyframe(duration, end)
    }

    fun sample(trackName: String, time: Float = currentTime): Vec3 {
        return tracks[trackName]?.sample(time) ?: Vec3(0f, 0f, 0f)
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
    var position: Vec3,
    var velocity: Vec3 = Vec3(0f, 0f, 0f),
    var radius: Float = 24f
) {
    fun step(dt: Float, gravity: Float) {
        velocity.y += gravity * dt
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        position.z += velocity.z * dt
    }

    fun applyBounds(width: Float, height: Float, depth: Float = 120f) {
        if (position.x < -width * 0.5f + radius) {
            position.x = -width * 0.5f + radius
            velocity.x *= -0.8f
        }
        if (position.x > width * 0.5f - radius) {
            position.x = width * 0.5f - radius
            velocity.x *= -0.8f
        }
        if (position.y < -height * 0.5f + radius) {
            position.y = -height * 0.5f + radius
            velocity.y *= -0.8f
        }
        if (position.y > height * 0.5f - radius) {
            position.y = height * 0.5f - radius
            velocity.y *= -0.8f
        }
        if (position.z < -depth * 0.5f + radius) {
            position.z = -depth * 0.5f + radius
            velocity.z *= -0.8f
        }
        if (position.z > depth * 0.5f - radius) {
            position.z = depth * 0.5f - radius
            velocity.z *= -0.8f
        }
    }
}

class MeshBinding(
    val surfaceMesh: List<Vec3>,
    val collisionMesh: List<Vec3>,
    val weightPaint: WeightPaint,
    var transform: Vec3 = Vec3(0f, 0f, 0f),
    var rotation: Vec3 = Vec3(0f, 0f, 0f),
    val bindingMode: BindingMode = BindingMode.Partial
) {
    fun blend(animationPose: Vec3, physicsPose: Vec3, weight: Float): Vec3 {
        return Vec3(
            animationPose.x * (1f - weight) + physicsPose.x * weight,
            animationPose.y * (1f - weight) + physicsPose.y * weight,
            animationPose.z * (1f - weight) + physicsPose.z * weight
        )
    }

    fun paintedWeight(index: Int): Float = when (bindingMode) {
        BindingMode.Full -> 1f
        BindingMode.Partial -> weightPaint.get(index).coerceIn(0f, 1f)
        BindingMode.PhysicsDriven -> 1f
    }

    fun rotatePoint(vertex: Vec3): Vec3 {
        val x = vertex.x
        val y = vertex.y
        val z = vertex.z

        val ax = rotation.x
        val ay = rotation.y
        val az = rotation.z

        val cosX = cos(ax)
        val sinX = sin(ax)
        val cosY = cos(ay)
        val sinY = sin(ay)
        val cosZ = cos(az)
        val sinZ = sin(az)

        val y1 = y * cosX - z * sinX
        val z1 = y * sinX + z * cosX
        val x2 = x * cosY + z1 * sinY
        val z2 = -x * sinY + z1 * cosY
        val x3 = x2 * cosZ - y1 * sinZ
        val y3 = x2 * sinZ + y1 * cosZ

        return Vec3(x3, y3, z2)
    }

    fun finalSurface(animationPose: Vec3, physicsPose: Vec3): List<Vec3> {
        return surfaceMesh.mapIndexed { index, vertex ->
            val weight = paintedWeight(index)
            val blended = blend(animationPose, physicsPose, weight)
            val rotated = rotatePoint(vertex)
            Vec3(
                rotated.x + blended.x + transform.x,
                rotated.y + blended.y + transform.y,
                rotated.z + blended.z + transform.z
            )
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
        proxy.applyBounds(900f, 600f)
        animationTimeline.update(dt)
    }

    fun sampleRigPose(): Vec3 {
        val primary = animationTimeline.sample("position")
        val secondary = animationTimeline.sample("secondary")
        return Vec3(primary.x + secondary.x, primary.y + secondary.y, primary.z + secondary.z)
    }

    fun finalSurface(): List<Vec3> {
        val animationPose = sampleRigPose()
        val physicsPose = proxy.position
        return meshBinding.finalSurface(animationPose, physicsPose)
    }

    fun projectToScreen(width: Float, height: Float, cameraDistance: Float = 600f, focalLength: Float = 560f): List<Vec2> {
        val zBias = cameraDistance
        return finalSurface().map { point ->
            val z = point.z + zBias
            val scale = focalLength / z.coerceAtLeast(1f)
            Vec2(
                point.x * scale + width * 0.5f,
                point.y * scale + height * 0.5f
            )
        }
    }

    fun setPlaying(enabled: Boolean) {
        animationTimeline.playing = enabled
    }

    fun setLoopPose(start: Vec3, end: Vec3, duration: Float) {
        animationTimeline.setLoopPose("position", start, end, duration)
    }

    fun setTime(time: Float) {
        animationTimeline.currentTime = time
    }

    fun setSecondaryMotion(start: Vec3, mid: Vec3, end: Vec3, duration: Float) {
        animationTimeline.addMultiTrackPose("secondary", start, mid, end, duration)
    }
}

class SceneRigBuilder {
    fun createScene(): MeshRigScene {
        val timeline = AnimationTimeline().apply {
            setLoopPose("position", Vec3(0f, 0f, 0f), Vec3(40f, 18f, 25f), 2f)
            addMultiTrackPose("secondary", Vec3(0f, 0f, 0f), Vec3(12f, -18f, 8f), Vec3(18f, 30f, 12f), 1.5f)
            play()
        }

        val proxy = PhysicsProxy(
            position = Vec3(0f, 0f, 20f),
            velocity = Vec3(18f, 10f, 12f),
            radius = 28f
        )

        val points = listOf(
            Vec3(-38f, -26f, -18f),
            Vec3(12f, -52f, -10f),
            Vec3(50f, -28f, -22f),
            Vec3(62f, 0f, 0f),
            Vec3(52f, 24f, 10f),
            Vec3(18f, 40f, 14f),
            Vec3(-10f, 52f, 20f),
            Vec3(-42f, 38f, 12f),
            Vec3(-58f, 8f, -12f),
            Vec3(-48f, -16f, -26f)
        )

        val weightPaint = WeightPaint().apply {
            setRange(0, 1, 0.30f)
            setRange(2, 4, 0.55f)
            setRange(5, 6, 0.78f)
            setRange(7, 9, 0.88f)
            setSoftFalloff(4, 2, 0.72f)
        }

        val meshBinding = MeshBinding(
            surfaceMesh = points,
            collisionMesh = points,
            weightPaint = weightPaint,
            bindingMode = BindingMode.Partial,
            transform = Vec3(0f, 0f, 0f),
            rotation = Vec3(0.18f, 0.28f, 0.12f)
        )

        return MeshRigScene(
            animationTimeline = timeline,
            proxy = proxy,
            meshBinding = meshBinding
        )
    }
}
