package com.example.physicsengine.physics

import com.example.physicsengine.animation.Vec2

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

    fun applyCollisionBounds(worldWidth: Float, worldHeight: Float) {
        if (position.x < radius) {
            position.x = radius
            velocity.x *= -0.8f
        }
        if (position.x > worldWidth - radius) {
            position.x = worldWidth - radius
            velocity.x *= -0.8f
        }
        if (position.y < radius) {
            position.y = radius
            velocity.y *= -0.8f
        }
        if (position.y > worldHeight - radius) {
            position.y = worldHeight - radius
            velocity.y *= -0.8f
        }
    }
}

class PhysicsProxyRig(
    private val proxy: PhysicsProxy,
    private val finalTransform: (Vec2) -> Unit
) {
    fun syncToRender() {
        finalTransform(proxy.position)
    }
}
