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

    fun setLoopingPose(trackName: String, start: Vec2, end: Vec2, duration: Float) {
        tracks.remove(trackName)
        tracks[trackName] = mutableListOf(
            Keyframe(0f, start),
            Keyframe(duration, end)
        )
    }

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

    fun weightAt(index: Int): Float = vertices.firstOrNull { it.index == index }?.weight ?: 0f
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
    val physicsTarget: Vec2,
    private val blendStrength: Float = 0.6f
) {
    fun blendPose(): Vec2 {
        val animated = animationTimeline.poseFor("position")
        return Vec2(
            animated.x * (1f - blendStrength) + physicsTarget.x * blendStrength,
            animated.y * (1f - blendStrength) + physicsTarget.y * blendStrength
        )
    }

    fun applyToRender() {
        val blended = blendPose()
        renderBinding.position = blended
    }
}

class AnimationRig(
    val timeline: AnimationTimeline,
    val meshBinding: RenderBinding,
    val physicsTarget: Vec2
) {
    private val binding = AnimationPhysicsBinding(meshBinding, timeline, physicsTarget)

    fun updatePhysicsTarget(target: Vec2) {
        binding.applyToRender()
        meshBinding.position = binding.blendPose()
    }

    fun addMoveKeyframe(time: Float, position: Vec2) {
        timeline.addKeyframe("position", time, position)
    }

    fun sampleCurrentPose(): Vec2 = timeline.poseFor("position")
}

enum class BindingMode {
    Full,
    Partial,
    PhysicsDriven
}

data class WeightVertex(
    val index: Int,
    val weight: Float
)

data class WeightPaint(
    val vertices: MutableList<WeightVertex> = mutableListOf()
) {
    fun add(index: Int, weight: Float) {
        vertices.add(WeightVertex(index, weight))
    }

    fun weightOf(index: Int): Float = vertices.firstOrNull { it.index == index }?.weight ?: 0f
}

data class MeshBinding(
    val surfaceMesh: List<Vec2>,
    val collisionMesh: List<Vec2>,
    val weightPaint: WeightPaint,
    val bindingMode: BindingMode = BindingMode.Partial,
    var transform: Vec2 = Vec2(0f, 0f),
    var rotation: Float = 0f,
    val color: Color = Color(0xFF72D5FF)
) {
    fun transformedSurface(): List<Vec2> {
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()
        return surfaceMesh.map { v ->
            val x = (v.x * c - v.y * s) + transform.x
            val y = (v.x * s + v.y * c) + transform.y
            Vec2(x, y)
        }
    }
}
