package com.example.physicsengine

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

class AnimationTimeline {
    var currentTime: Float = 0f
    var playing: Boolean = false
    var loop: Boolean = true
    var interpolation: Interpolation = Interpolation.Smooth
    val tracks: MutableMap<String, MutableList<Keyframe>> = linkedMapOf()

    fun addKeyframe(trackName: String, time: Float, value: Vec2) {
        val list = tracks.getOrPut(trackName) { mutableListOf() }
        list.add(Keyframe(time, value))
        list.sortBy { it.time }
    }

    fun sample(trackName: String, time: Float): Vec2 {
        val keys = tracks[trackName] ?: return Vec2(0f, 0f)
        if (keys.isEmpty()) return Vec2(0f, 0f)
        if (keys.size == 1) return keys.first().value

        var prev = keys.first()
        var next = keys.first()

        for (key in keys) {
            if (key.time <= time) prev = key
            if (key.time >= time) {
                next = key
                break
            }
        }

        if (prev.time == next.time) return prev.value
        val t = ((time - prev.time) / (next.time - prev.time)).coerceIn(0f, 1f)
        val blended = when (interpolation) {
            Interpolation.Linear -> Vec2(
                prev.value.x + (next.value.x - prev.value.x) * t,
                prev.value.y + (next.value.y - prev.value.y) * t
            )
            Interpolation.Smooth -> {
                val smooth = t * t * (3f - 2f * t)
                Vec2(
                    prev.value.x + (next.value.x - prev.value.x) * smooth,
                    prev.value.y + (next.value.y - prev.value.y) * smooth
                )
            }
            Interpolation.Hold -> prev.value
        }
        return blended
    }

    fun update(dt: Float) {
        if (!playing) return
        currentTime += dt
        val maxTime = tracks.values.flatten().maxOfOrNull { it.time } ?: 0f
        if (loop && maxTime > 0f && currentTime > maxTime) {
            currentTime = 0f
        }
    }

    fun poseFor(trackName: String): Vec2 = sample(trackName, currentTime)

    enum class Interpolation {
        Linear,
        Smooth,
        Hold
    }

    data class Keyframe(
        val time: Float,
        val value: Vec2
    )
}

data class Vec2(
    var x: Float,
    var y: Float
)

data class WeightVertex(
    val index: Int,
    val weight: Float
)

data class PhysicsWeightPaint(
    val vertices: MutableList<WeightVertex> = mutableListOf()
) {
    fun add(index: Int, weight: Float) {
        vertices.add(WeightVertex(index, weight))
    }
}

data class RenderBinding(
    val meshVertices: List<Vec2>,
    val collisionVertices: List<Vec2>,
    val weightPaint: PhysicsWeightPaint,
    var position: Vec2,
    var rotation: Float = 0f,
    var scale: Vec2 = Vec2(1f, 1f),
    val color: Color = Color(0xFF72D5FF)
) {
    fun transformedVertices(): List<Vec2> {
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()
        return meshVertices.map { v ->
            val x = (v.x * scale.x * c - v.y * scale.y * s) + position.x
            val y = (v.x * scale.x * s + v.y * scale.y * c) + position.y
            Vec2(x, y)
        }
    }
}

class AnimationPhysicsBinding(
    val renderBinding: RenderBinding,
    val animationTimeline: AnimationTimeline,
    val physicsTarget: Vec2
) {
    fun blendPose(): Vec2 {
        val animated = animationTimeline.poseFor("position")
        val blend = 0.6f
        return Vec2(
            animated.x * (1f - blend) + physicsTarget.x * blend,
            animated.y * (1f - blend) + physicsTarget.y * blend
        )
    }
}
