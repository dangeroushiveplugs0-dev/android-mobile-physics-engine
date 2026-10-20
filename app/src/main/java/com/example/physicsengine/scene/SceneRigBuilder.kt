package com.example.physicsengine.scene

class SceneRigBuilder {
    fun createScene(): MeshRigScene {
        val timeline = AnimationTimeline().apply {
            setLoopPose("position", Vec2(110f, 180f), Vec2(260f, 150f), 2f)
            setLoopPose("secondary", Vec2(0f, 0f), Vec2(12f, -18f), 1.5f)
            play()
        }

        val proxy = PhysicsProxy(
            position = Vec2(150f, 190f),
            velocity = Vec2(35f, 18f),
            radius = 28f
        )

        val points = listOf(
            Vec2(-38f, -26f),
            Vec2(18f, -44f),
            Vec2(52f, -18f),
            Vec2(54f, 16f),
            Vec2(26f, 38f),
            Vec2(-10f, 46f),
            Vec2(-42f, 30f),
            Vec2(-52f, 4f)
        )

        val weightPaint = WeightPaint().apply {
            setRange(0, 1, 0.35f)
            setRange(2, 3, 0.6f)
            setRange(4, 5, 0.82f)
            setRange(6, 7, 0.9f)
        }

        val meshBinding = MeshBinding(
            surfaceMesh = points,
            collisionMesh = points,
            weightPaint = weightPaint,
            bindingMode = BindingMode.Partial,
            transform = Vec2(0f, 0f),
            rotation = 0.2f
        )

        return MeshRigScene(
            animationTimeline = timeline,
            proxy = proxy,
            meshBinding = meshBinding
        )
    }
}
