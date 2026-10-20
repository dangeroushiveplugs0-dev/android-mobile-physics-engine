package com.example.physicsengine.scene

class SceneRigBuilder {
    fun createScene(): MeshRigScene {
        val timeline = AnimationTimeline()
        timeline.addKeyframe("position", 0f, Vec2(100f, 200f))
        timeline.addKeyframe("position", 1f, Vec2(260f, 180f))
        timeline.addKeyframe("position", 2f, Vec2(180f, 220f))

        val proxy = PhysicsProxy(position = Vec2(150f, 190f), velocity = Vec2(50f, 20f), radius = 28f)

        val points = listOf(
            Vec2(-30f, -30f),
            Vec2(30f, -30f),
            Vec2(30f, 30f),
            Vec2(-30f, 30f)
        )

        val weightPaint = WeightPaint().apply {
            set(0, 0.4f)
            set(1, 0.6f)
            set(2, 0.8f)
            set(3, 0.5f)
        }

        val meshBinding = MeshBinding(
            surfaceMesh = points,
            collisionMesh = points,
            weightPaint = weightPaint,
            bindingMode = BindingMode.Partial
        )

        return MeshRigScene(
            animationTimeline = timeline,
            proxy = proxy,
            meshBinding = meshBinding
        )
    }
}
