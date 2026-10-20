package com.example.physicsengine.animation

import kotlin.math.max

enum class BindingMode {
    Full,
    Partial,
    PhysicsDriven
}

data class WeightVertex(
    val index: Int,
    val weight: Float
)

class WeightPaint {
    private val values: MutableMap<Int, Float> = linkedMapOf()

    fun set(vertexIndex: Int, weight: Float) {
        values[vertexIndex] = weight.coerceIn(0f, 1f)
    }

    fun weightFor(vertexIndex: Int): Float = values[vertexIndex] ?: 0f

    fun clear() = values.clear()

    fun allWeights(): Map<Int, Float> = values.toMap()
}

class MeshInfluenceBinder(
    private val surfaceMesh: List<Vec2>,
    private val collisionMesh: List<Vec2>,
    private val weightPaint: WeightPaint,
    private val bindingMode: BindingMode = BindingMode.Partial
) {
    fun surfaceCount(): Int = surfaceMesh.size
    fun collisionCount(): Int = collisionMesh.size

    fun influenceAt(vertexIndex: Int): Float = weightPaint.weightFor(vertexIndex)

    fun evaluateBlend(animationPose: Vec2, physicsPose: Vec2, strength: Float): Vec2 {
        val blendedX = animationPose.x * (1f - strength) + physicsPose.x * strength
        val blendedY = animationPose.y * (1f - strength) + physicsPose.y * strength
        return Vec2(blendedX, blendedY)
    }

    fun appliedSurface(animationPose: Vec2, physicsPose: Vec2, strength: Float): List<Vec2> {
        val blended = evaluateBlend(animationPose, physicsPose, strength)
        return surfaceMesh.map { vertex ->
            Vec2(vertex.x + blended.x, vertex.y + blended.y)
        }
    }

    fun weightPaintedInfluence(vertexIndex: Int, animationPose: Vec2, physicsPose: Vec2, strength: Float): Vec2 {
        val weight = weightPaint.weightFor(vertexIndex)
        val finalStrength = when (bindingMode) {
            BindingMode.Full -> 1f
            BindingMode.Partial -> maxOf(0f, minOf(1f, weight))
            BindingMode.PhysicsDriven -> 1f
        }

        val mixed = evaluateBlend(animationPose, physicsPose, strength * finalStrength)
        return mixed
    }
}
