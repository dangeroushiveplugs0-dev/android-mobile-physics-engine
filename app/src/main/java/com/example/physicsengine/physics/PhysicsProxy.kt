package com.example.physicsengine.animation

import kotlin.math.cos
import kotlin.math.sin

class MeshBinding(
    val surfaceMesh: List<Vec2>,
    val collisionMesh: List<Vec2>,
    val weightPaint: WeightPaint,
    var transform: Vec2 = Vec2(0f, 0f),
    var rotation: Float = 0f,
    val bindingMode: BindingMode = BindingMode.Partial
) {
    fun transformedSurface(): List<Vec2> {
        val c = cos(rotation.toDouble()).toFloat()
        val s = sin(rotation.toDouble()).toFloat()

        return surfaceMesh.map { vertex ->
            val x = (vertex.x * c - vertex.y * s) + transform.x
            val y = (vertex.x * s + vertex.y * c) + transform.y
            Vec2(x, y)
        }
    }

    fun blend(animationPose: Vec2, physicsPose: Vec2, strength: Float): Vec2 {
        val blendedX = animationPose.x * (1f - strength) + physicsPose.x * strength
        val blendedY = animationPose.y * (1f - strength) + physicsPose.y * strength
        return Vec2(blendedX, blendedY)
    }
}

class MeshBindingRig(
    private val animationTimeline: AnimationTimeline,
    private val meshBinding: MeshBinding,
    private val physicsTarget: Vec2,
    private val blendStrength: Float = 0.6f
) {
    fun currentAnimationPose(): Vec2 = animationTimeline.sample("position")

    fun currentBlendedPose(): Vec2 = meshBinding.blend(currentAnimationPose(), physicsTarget, blendStrength)

    fun updatePose() {
        meshBinding.transform = currentBlendedPose()
    }

    fun setPhysicsTarget(target: Vec2) {
        meshBinding.transform = meshBinding.blend(currentAnimationPose(), target, blendStrength)
    }
}

class AnimationPoseController(
    private val timeline: AnimationTimeline
) {
    fun addPositionKeyframe(time: Float, position: Vec2) {
        timeline.addKeyframe("position", time, position)
    }

    fun play() {
        timeline.playing = true
    }

    fun stop() {
        timeline.playing = false
    }
}
