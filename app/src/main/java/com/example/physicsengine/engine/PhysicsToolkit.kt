package com.example.physicsengine.engine

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private fun Vec2.dot(other: Vec2): Float = x * other.x + y * other.y

private fun Vec2.lengthSq(): Float = x * x + y * y

private fun Vec2.length(): Float = sqrt(lengthSq().toDouble()).toFloat()

private fun Vec2.normalized(): Vec2 {
    val len = length()
    return if (len > 0f) Vec2(x / len, y / len) else Vec2(0f, 0f)
}

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(s: Float): Vec2 = Vec2(x * s, y * s)
    operator fun div(s: Float): Vec2 = Vec2(x / s, y / s)
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)
    fun dot(other: Vec2): Float = x * other.x + y * other.y
}

enum class BodyType { Dynamic, Static, Kinematic }

data class MeshFace(
    val vertices: List<Vec2>,
    val uv: List<Vec2> = emptyList()
) {
    fun asTriangles(): List<List<Vec2>> {
        if (vertices.size < 3) return emptyList()
        if (vertices.size == 3) return listOf(vertices)
        val triangles = mutableListOf<List<Vec2>>()
        val fanStart = vertices.first()
        for (i in 1 until vertices.size - 1) {
            triangles += listOf(fanStart, vertices[i], vertices[i + 1])
        }
        return triangles
    }
}

data class CollisionMesh(
    val localVertices: List<Vec2>,
    val faces: List<MeshFace> = emptyList()
) {
    fun worldVertices(position: Vec2, rotation: Float, scale: Vec2): List<Vec2> {
        val cos = kotlin.math.cos(rotation)
        val sin = kotlin.math.sin(rotation)
        return localVertices.map { p ->
            val sx = p.x * scale.x
            val sy = p.y * scale.y
            val x = (sx * cos - sy * sin) + position.x
            val y = (sx * sin + sy * cos) + position.y
            Vec2(x, y)
        }
    }
}

class PhysicsBody(
    val id: String,
    var position: Vec2,
    var velocity: Vec2 = Vec2(0f, 0f),
    var radius: Float = 18f,
    var mass: Float = 1f,
    var bodyType: BodyType = BodyType.Dynamic,
    var color: Color = Color(0xFF72D5FF),
    var renderVertices: List<Vec2> = emptyList(),
    var collisionMesh: CollisionMesh? = null,
    var rotation: Float = 0f,
    var scale: Vec2 = Vec2(1f, 1f)
) {
    fun transformedRenderVertices(): List<Vec2> {
        if (renderVertices.isEmpty()) {
            val v = radius * 0.75f
            return listOf(
                Vec2(-v, -v),
                Vec2(v, -v),
                Vec2(v, v),
                Vec2(-v, v)
            ).map { it + position }
        }
        val cos = kotlin.math.cos(rotation)
        val sin = kotlin.math.sin(rotation)
        return renderVertices.map { p ->
            val sx = p.x * scale.x
            val sy = p.y * scale.y
            val x = (sx * cos - sy * sin) + position.x
            val y = (sx * sin + sy * cos) + position.y
            Vec2(x, y)
        }
    }

    fun transformedCollisionVertices(): List<Vec2> {
        if (collisionMesh == null) {
            return transformedRenderVertices()
        }
        return collisionMesh!!.worldVertices(position, rotation, scale)
    }

    fun isCircleLike(): Boolean = collisionMesh == null && renderVertices.isEmpty()
}

class PhysicsWorld(
    var width: Float = 800f,
    var height: Float = 1200f,
    var gravity: Vec2 = Vec2(0f, 1100f)
) {
    val bodies = mutableListOf<PhysicsBody>()
    private var dragBodyId: String? = null

    fun resetScene() {
        bodies.clear()
        val box = CollisionMesh(
            localVertices = listOf(
                Vec2(-36f, -20f),
                Vec2(36f, -20f),
                Vec2(36f, 20f),
                Vec2(-36f, 20f)
            )
        )
        val diamond = CollisionMesh(
            localVertices = listOf(
                Vec2(0f, -45f),
                Vec2(42f, 0f),
                Vec2(0f, 45f),
                Vec2(-42f, 0f)
            )
        )

        val left = PhysicsBody(
            id = "box_left",
            position = Vec2(180f, 120f),
            velocity = Vec2(120f, 30f),
            radius = 36f,
            mass = 2f,
            color = Color(0xFF72D5FF),
            collisionMesh = box,
            renderVertices = listOf(
                Vec2(-36f, -20f),
                Vec2(36f, -20f),
                Vec2(36f, 20f),
                Vec2(-36f, 20f)
            )
        )

        val right = PhysicsBody(
            id = "diamond_right",
            position = Vec2(540f, 150f),
            velocity = Vec2(-110f, -20f),
            radius = 42f,
            mass = 2.4f,
            color = Color(0xFF8CF7B8),
            collisionMesh = diamond,
            renderVertices = listOf(
                Vec2(0f, -45f),
                Vec2(42f, 0f),
                Vec2(0f, 45f),
                Vec2(-42f, 0f)
            )
        )

        val floor = PhysicsBody(
            id = "ground",
            position = Vec2(width / 2f, height - 22f),
            velocity = Vec2(0f, 0f),
            radius = 350f,
            mass = Float.MAX_VALUE,
            bodyType = BodyType.Static,
            color = Color(0xFF202A3F),
            renderVertices = listOf(
                Vec2(-300f, -18f),
                Vec2(300f, -18f),
                Vec2(300f, 18f),
                Vec2(-300f, 18f)
            ),
            collisionMesh = CollisionMesh(
                localVertices = listOf(
                    Vec2(-300f, -18f),
                    Vec2(300f, -18f),
                    Vec2(300f, 18f),
                    Vec2(-300f, 18f)
                )
            )
        )

        bodies += left
        bodies += right
        bodies += floor
    }

    fun bindMeshBody(
        id: String,
        visibleVertices: List<Vec2>,
        collisionVertices: List<Vec2>? = null,
        position: Vec2,
        velocity: Vec2 = Vec2(0f, 0f),
        rotation: Float = 0f,
        mass: Float = 1f,
        color: Color = Color(0xFF72D5FF),
        bodyType: BodyType = BodyType.Dynamic,
        radius: Float = 32f
    ): PhysicsBody {
        val proxy = collisionVertices ?: visibleVertices
        val body = PhysicsBody(
            id = id,
            position = position,
            velocity = velocity,
            radius = radius,
            mass = mass,
            bodyType = bodyType,
            color = color,
            renderVertices = visibleVertices,
            collisionMesh = CollisionMesh(proxy),
            rotation = rotation
        )
        bodies += body
        return body
    }

    fun dragBody(worldX: Float, worldY: Float): PhysicsBody? {
        val hit = bodies.asReversed().firstOrNull { body ->
            val vertices = body.transformedCollisionVertices()
            pointInPolygon(worldX, worldY, vertices)
        }
        if (hit != null) {
            dragBodyId = hit.id
            hit.bodyType = BodyType.Kinematic
            hit.position = Vec2(worldX, worldY)
            hit.velocity = Vec2(0f, 0f)
        }
        return hit
    }

    fun releaseDrag() {
        dragBodyId?.let { id ->
            val body = bodies.firstOrNull { it.id == id }
            if (body != null) body.bodyType = BodyType.Dynamic
        }
        dragBodyId = null
    }

    fun update(dt: Float) {
        val fixedDt = 1f / 120f
        var accumulator = dt
        while (accumulator > 0f) {
            val step = min(fixedDt, accumulator)
            stepSimulation(step)
            accumulator -= step
        }
    }

    private fun stepSimulation(dt: Float) {
        for (body in bodies) {
            if (body.bodyType == BodyType.Static) continue
            if (body.id == dragBodyId) continue
            body.velocity += gravity * dt
            body.position += body.velocity * dt
            resolveWorldBounds(body)
        }
        resolveBodyCollisions()
    }

    private fun resolveWorldBounds(body: PhysicsBody) {
        if (body.bodyType == BodyType.Static) return
        val radius = body.radius
        if (body.position.x < radius) {
            body.position.x = radius
            body.velocity.x = abs(body.velocity.x) * 0.78f
        } else if (body.position.x > width - radius) {
            body.position.x = width - radius
            body.velocity.x = -abs(body.velocity.x) * 0.78f
        }

        if (body.position.y < radius) {
            body.position.y = radius
            body.velocity.y = abs(body.velocity.y) * 0.78f
        } else if (body.position.y > height - radius) {
            body.position.y = height - radius
            body.velocity.y = -abs(body.velocity.y) * 0.78f
            body.velocity.x *= 0.995f
        }
    }

    private fun resolveBodyCollisions() {
        for (i in 0 until bodies.size) {
            val a = bodies[i]
            if (a.bodyType == BodyType.Static) continue
            for (j in i + 1 until bodies.size) {
                val b = bodies[j]
                if (b.bodyType == BodyType.Static) continue
                if (a.isCircleLike() && b.isCircleLike()) {
                    handleCircleCollision(a, b)
                } else {
                    handlePolygonCollision(a, b)
                }
            }
        }
    }

    private fun handleCircleCollision(a: PhysicsBody, b: PhysicsBody) {
        val dx = b.position.x - a.position.x
        val dy = b.position.y - a.position.y
        val distance = sqrt(dx * dx + dy * dy)
        val minDist = a.radius + b.radius
        if (distance >= minDist) return

        val nx = if (distance > 0f) dx / distance else 1f
        val ny = if (distance > 0f) dy / distance else 0f
        val overlap = minDist - distance
        val invMassA = if (a.mass <= 0f) 0f else 1f / a.mass
        val invMassB = if (b.mass <= 0f) 0f else 1f / b.mass
        val totalInvMass = invMassA + invMassB
        if (totalInvMass <= 0f) return

        a.position -= Vec2(nx * overlap * invMassA, ny * overlap * invMassA)
        b.position += Vec2(nx * overlap * invMassB, ny * overlap * invMassB)

        val rvx = b.velocity.x - a.velocity.x
        val rvy = b.velocity.y - a.velocity.y
        val velAlongNormal = rvx * nx + rvy * ny
        if (velAlongNormal > 0f) return

        val restitution = 0.82f
        val impulse = (-(1f + restitution) * velAlongNormal) / totalInvMass
        val ix = impulse * nx
        val iy = impulse * ny
        a.velocity -= Vec2(ix * invMassA, iy * invMassA)
        b.velocity += Vec2(ix * invMassB, iy * invMassB)
    }

    private fun handlePolygonCollision(a: PhysicsBody, b: PhysicsBody) {
        val aVerts = a.transformedCollisionVertices()
        val bVerts = b.transformedCollisionVertices()
        val axes = collectAxes(aVerts) + collectAxes(bVerts)
        var minimumOverlap = Float.MAX_VALUE
        var smallestAxis = Vec2(0f, 0f)
        var hasOverlap = true

        for (axis in axes) {
            val (aMin, aMax) = projectVertices(aVerts, axis)
            val (bMin, bMax) = projectVertices(bVerts, axis)
            val overlap = min(aMax, bMax) - max(aMin, bMin)
            if (overlap < 0f) {
                hasOverlap = false
                break
            }
            if (overlap < minimumOverlap) {
                minimumOverlap = overlap
                smallestAxis = axis.normalized()
            }
        }

        if (!hasOverlap) return

        val delta = b.position - a.position
        if (delta.dot(smallestAxis) < 0f) {
            smallestAxis = -smallestAxis
        }

        val invMassA = if (a.mass <= 0f) 0f else 1f / a.mass
        val invMassB = if (b.mass <= 0f) 0f else 1f / b.mass
        val totalInvMass = invMassA + invMassB
        if (totalInvMass <= 0f) return

        val correction = minimumOverlap / totalInvMass
        a.position -= smallestAxis * correction * invMassA
        b.position += smallestAxis * correction * invMassB

        val relativeVel = b.velocity - a.velocity
        val velAlongNormal = relativeVel.dot(smallestAxis)
        if (velAlongNormal > 0f) return
        val restitution = 0.8f
        val impulse = (-(1f + restitution) * velAlongNormal) / totalInvMass
        val impulseVec = smallestAxis * impulse
        a.velocity -= impulseVec * invMassA
        b.velocity += impulseVec * invMassB
    }

    private fun collectAxes(vertices: List<Vec2>): List<Vec2> {
        if (vertices.size < 2) return emptyList()
        val axes = mutableListOf<Vec2>()
        for (i in vertices.indices) {
            val current = vertices[i]
            val next = vertices[(i + 1) % vertices.size]
            val edge = next - current
            val axis = Vec2(-edge.y, edge.x).normalized()
            if (axis.length() > 0f) axes += axis
        }
        return axes
    }

    private fun projectVertices(vertices: List<Vec2>, axis: Vec2): Pair<Float, Float> {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (v in vertices) {
            val dot = v.dot(axis)
            min = min(min, dot)
            max = max(max, dot)
        }
        return min to max
    }

    private fun pointInPolygon(px: Float, py: Float, vertices: List<Vec2>): Boolean {
        var inside = false
        var j = vertices.size - 1
        for (i in vertices.indices) {
            val xi = vertices[i].x
            val yi = vertices[i].y
            val xj = vertices[j].x
            val yj = vertices[j].y
            val intersects = ((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / (yj - yi + 1e-6f) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
}

fun createHexagon(radius: Float): List<Vec2> {
    val points = mutableListOf<Vec2>()
    repeat(6) { i ->
        val angle = (Math.PI * 2.0 * i / 6.0).toFloat()
        points += Vec2(
            kotlin.math.cos(angle) * radius,
            kotlin.math.sin(angle) * radius
        )
    }
    return points
}

fun createBox(width: Float, height: Float): List<Vec2> = listOf(
    Vec2(-width / 2f, -height / 2f),
    Vec2(width / 2f, -height / 2f),
    Vec2(width / 2f, height / 2f),
    Vec2(-width / 2f, height / 2f)
)

fun createDiamond(width: Float, height: Float): List<Vec2> = listOf(
    Vec2(0f, -height / 2f),
    Vec2(width / 2f, 0f),
    Vec2(0f, height / 2f),
    Vec2(-width / 2f, 0f)
)

fun createArrow(): List<Vec2> = listOf(
    Vec2(-32f, -18f),
    Vec2(14f, -18f),
    Vec2(14f, -38f),
    Vec2(48f, 0f),
    Vec2(14f, 38f),
    Vec2(14f, 18f),
    Vec2(-32f, 18f)
)

fun meshTint(index: Int): Color {
    val palette = listOf(
        Color(0xFF72D5FF),
        Color(0xFF8CF7B8),
        Color(0xFFFFB86C),
        Color(0xFFD69CFF),
        Color(0xFFFF7E8A),
        Color(0xFF9DFFB0)
    )
    return palette[index % palette.size]
}

