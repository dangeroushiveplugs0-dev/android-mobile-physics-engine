package com.example.physicsengine

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

class AnimationTimeline {
    var currentTime: Float = 0f
    var playing: Boolean = false
    var loop: Boolean = true
    var interpolation: Interpolation = Interpolation.Smooth

    private val tracks: MutableMap<String, MutableList<Keyframe>> = linkedMapOf()

    fun addKeyframe(trackName: String, time: Float, value: PhysicsScene.Vec2) {
        val list = tracks.getOrPut(trackName) { mutableListOf() }
        list.add(Keyframe(time, value))
        list.sortBy { it.time }
    }

    fun clearTrack(trackName: String) {
        tracks.remove(trackName)
    }

    fun setLoopPose(trackName: String, start: PhysicsScene.Vec2, end: PhysicsScene.Vec2, duration: Float) {
        clearTrack(trackName)
        tracks[trackName] = mutableListOf(
            Keyframe(0f, start),
            Keyframe(duration, end)
        )
    }

    fun sample(trackName: String, time: Float): PhysicsScene.Vec2 {
        val keys = tracks[trackName] ?: return PhysicsScene.Vec2(0f, 0f)
        if (keys.isEmpty()) return PhysicsScene.Vec2(0f, 0f)
        if (keys.size == 1) return keys.first().value

        var previous = keys.first()
        var next = keys.first()

        for (key in keys) {
            if (key.time <= time) {
                previous = key
            }
            if (key.time >= time) {
                next = key
                break
            }
        }

        if (previous.time == next.time) return previous.value

        val t = ((time - previous.time) / (next.time - previous.time)).coerceIn(0f, 1f)
        return when (interpolation) {
            Interpolation.Linear -> PhysicsScene.Vec2(
                previous.value.x + (next.value.x - previous.value.x) * t,
                previous.value.y + (next.value.y - previous.value.y) * t
            )
            Interpolation.Smooth -> {
                val smooth = t * t * (3f - 2f * t)
                PhysicsScene.Vec2(
                    previous.value.x + (next.value.x - previous.value.x) * smooth,
                    previous.value.y + (next.value.y - previous.value.y) * smooth
                )
            }
            Interpolation.Hold -> previous.value
        }
    }

    fun poseFor(trackName: String): PhysicsScene.Vec2 = sample(trackName, currentTime)

    fun update(dt: Float) {
        if (!playing) return
        currentTime += dt
        val maxTime = tracks.values.flatten().maxOfOrNull { it.time } ?: 0f
        if (loop && maxTime > 0f && currentTime > maxTime) {
            currentTime = 0f
        }
    }

    enum class Interpolation {
        Linear,
        Smooth,
        Hold
    }

    data class Keyframe(
        val time: Float,
        val value: PhysicsScene.Vec2
    )
}

data class WeightVertex(
    val index: Int,
    val weight: Float
)

data class WeightPaint(
    private val weights: MutableMap<Int, Float> = linkedMapOf()
) {
    fun set(index: Int, weight: Float) {
        weights[index] = weight.coerceIn(0f, 1f)
    }

    fun weightFor(index: Int): Float = weights[index] ?: 0f

    fun clear() = weights.clear()
}

enum class BindingMode {
    Full,
    Partial,
    PhysicsDriven
}

data class MeshBinding(
    val surfaceMesh: List<PhysicsScene.Vec2>,
    val collisionMesh: List<PhysicsScene.Vec2>,
    val weightPaint: WeightPaint,
    val bindingMode: BindingMode = BindingMode.Partial,
    var transform: PhysicsScene.Vec2 = PhysicsScene.Vec2(0f, 0f),
    var rotation: Float = 0f,
    val color: Color = Color(0xFF72D5FF)
) {
    fun transformedSurface(): List<PhysicsScene.Vec2> {
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()
        return surfaceMesh.map { v ->
            val x = (v.x * c - v.y * s) + transform.x
            val y = (v.x * s + v.y * c) + transform.y
            PhysicsScene.Vec2(x, y)
        }
    }

    fun blendPosition(animationPosition: PhysicsScene.Vec2, physicsPosition: PhysicsScene.Vec2, strength: Float): PhysicsScene.Vec2 {
        val blendedX = animationPosition.x * (1f - strength) + physicsPosition.x * strength
        val blendedY = animationPosition.y * (1f - strength) + physicsPosition.y * strength
        return PhysicsScene.Vec2(blendedX, blendedY)
    }
}

class AnimationRig(
    private val timeline: AnimationTimeline,
    private val meshBinding: MeshBinding,
    private val physicsTarget: PhysicsScene.Vec2,
    private val blendStrength: Float = 0.6f
) {
    fun updatePhysicsTarget(target: PhysicsScene.Vec2) {
        val animatedPosition = timeline.poseFor("position")
        val finalPosition = meshBinding.blendPosition(animatedPosition, target, blendStrength)
        meshBinding.transform = finalPosition
    }

    fun addKeyframe(time: Float, position: PhysicsScene.Vec2) {
        timeline.addKeyframe("position", time, position)
    }

    fun sampleCurrentPose(): PhysicsScene.Vec2 = timeline.poseFor("position")

    fun currentBlendedPose(): PhysicsScene.Vec2 {
        val animated = timeline.poseFor("position")
        return meshBinding.blendPosition(animated, physicsTarget, blendStrength)
    }
}
