package com.example.physicsengine.animation

import kotlin.math.max
import kotlin.math.min

// Shared 2D vector used by animation and binding layers.
data class Vec2(
    var x: Float,
    var y: Float
) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vec2 = Vec2(x / scalar, y / scalar)
}

data class Keyframe(
    val time: Float,
    val value: Vec2
)

enum class InterpolationMode {
    Linear,
    Smooth,
    Hold
}

class AnimationTrack(
    val name: String,
    val interpolation: InterpolationMode = InterpolationMode.Smooth,
    private val keyframes: MutableList<Keyframe> = mutableListOf()
) {
    fun addKeyframe(time: Float, value: Vec2) {
        keyframes.add(Keyframe(time, value))
        keyframes.sortBy { it.time }
    }

    fun clear() = keyframes.clear()

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
        return when (interpolation) {
            InterpolationMode.Linear -> {
                val dx = next.value.x - previous.value.x
                val dy = next.value.y - previous.value.y
                Vec2(previous.value.x + dx * t, previous.value.y + dy * t)
            }
            InterpolationMode.Smooth -> {
                val smoothT = t * t * (3f - 2f * t)
                val dx = next.value.x - previous.value.x
                val dy = next.value.y - previous.value.y
                Vec2(previous.value.x + dx * smoothT, previous.value.y + dy * smoothT)
            }
            InterpolationMode.Hold -> previous.value
        }
    }

    fun duration(): Float = if (keyframes.isEmpty()) 0f else keyframes.last().time
}

class AnimationTimeline {
    private val tracks: MutableMap<String, AnimationTrack> = linkedMapOf()
    var currentTime: Float = 0f
    var playing: Boolean = false
    var loop: Boolean = true

    fun addTrack(name: String, interpolation: InterpolationMode = InterpolationMode.Smooth): AnimationTrack {
        val track = AnimationTrack(name, interpolation)
        tracks[name] = track
        return track
    }

    fun track(name: String): AnimationTrack? = tracks[name]

    fun addKeyframe(name: String, time: Float, value: Vec2, interpolation: InterpolationMode = InterpolationMode.Smooth) {
        val track = tracks[name] ?: addTrack(name, interpolation)
        track.addKeyframe(time, value)
    }

    fun setLoopPose(name: String, start: Vec2, end: Vec2, duration: Float, interpolation: InterpolationMode = InterpolationMode.Smooth) {
        val track = tracks[name] ?: addTrack(name, interpolation)
        track.clear()
        track.addKeyframe(0f, start)
        track.addKeyframe(duration, end)
    }

    fun sample(trackName: String): Vec2 = tracks[trackName]?.sample(currentTime) ?: Vec2(0f, 0f)

    fun update(dt: Float) {
        if (!playing) return
        currentTime += dt

        val maxDuration = tracks.values.fold(0f) { acc, track -> max(acc, track.duration()) }
        if (loop && maxDuration > 0f && currentTime > maxDuration) {
            currentTime = 0f
        }
    }
}

data class Pose(
    val position: Vec2,
    val rotation: Float = 0f,
    val scale: Vec2 = Vec2(1f, 1f)
)

class AnimatedRig(
    private val timeline: AnimationTimeline,
    private val positionTrackName: String = "position"
) {
    fun pose(): Pose {
        val position = timeline.sample(positionTrackName)
        return Pose(position = position)
    }

    fun addPositionKeyframe(time: Float, value: Vec2) {
        timeline.addKeyframe(positionTrackName, time, value)
    }
}
