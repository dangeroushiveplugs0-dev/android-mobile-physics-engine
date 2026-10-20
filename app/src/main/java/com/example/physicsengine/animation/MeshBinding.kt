package com.example.physicsengine.animation

import kotlin.math.max
import kotlin.math.min

data class VertexInfluence(
    val vertexIndex: Int,
    val weight: Float
)

class WeightPaint {
    private val weights: MutableMap<Int, Float> = linkedMapOf()

    fun set(vertexIndex: Int, weight: Float) {
        weights[vertexIndex] = weight.coerceIn(0f, 1f)
    }

    fun remove(vertexIndex: Int) {
        weights.remove(vertexIndex)
    }

    fun weightFor(vertexIndex: Int): Float = weights[vertexIndex] ?: 0f

    fun allWeights(): Map<Int, Float> = weights.toMap()

    fun clear() = weights.clear()

    fun normalizedWeight(vertexIndex: Int): Float = when (val value = weightFor(vertexIndex)) {
        0f -> 0f
        else -> value.coerceIn(0f, 1f)
    }
}

enum class BindingMode {
    Full,
    Partial,
    PhysicsDriven
}

class MeshInfluenceBinder(
    private val weightPaint: WeightPaint,
    private val bindingMode: BindingMode = BindingMode.Partial
) {
    fun influenceFor(vertexIndex: Int): Float = weightPaint.weightFor(vertexIndex)

    fun effectiveInfluence(vertexIndex: Int): Float {
        val raw = influenceFor(vertexIndex)
        return when (bindingMode) {
            BindingMode.Full -> 1f
            BindingMode.Partial -> raw.coerceIn(0f, 1f)
            BindingMode.PhysicsDriven -> 1f
        }
    }

    fun blend(animationPose: Vec2, physicsPose: Vec2, strength: Float, vertexIndex: Int): Vec2 {
        val factor = effectiveInfluence(vertexIndex) * strength
        val x = animationPose.x * (1f - factor) + physicsPose.x * factor
        val y = animationPose.y * (1f - factor) + physicsPose.y * factor
        return Vec2(x, y)
    }
}

class WeightPaintLayer(
    private val surfaceSize: Int,
    private val startWeight: Float = 0f
) {
    private val weights: FloatArray = FloatArray(surfaceSize) { startWeight }

    fun set(vertexIndex: Int, value: Float) {
        if (vertexIndex in 0 until weights.size) {
            weights[vertexIndex] = value.coerceIn(0f, 1f)
        }
    }

    fun weightFor(vertexIndex: Int): Float = if (vertexIndex in 0 until weights.size) weights[vertexIndex] else 0f

    fun copyFrom(other: WeightPaintLayer) {
        for (index in weights.indices) {
            weights[index] = other.weightFor(index)
        }
    }
}
