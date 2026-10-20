package com.example.physicsengine.animation

import kotlin.math.max

class AnimationTimeline {
    private val tracks: MutableMap<String, AnimationTrack> = linkedMapOf()
    var currentTime: Float = 0f
    var playing: Boolean = false
    var loop: Boolean = true

    fun addTrack(name: String, interpolation: InterpolationMode = InterpolationMode.Smooth): AnimationTrack {
        val track = AnimationTrack(interpolation)
        tracks[name] = track
        return track
    }

    fun addKeyframe(trackName: String, time: Float, value: Vec2, interpolation: InterpolationMode = InterpolationMode.Smooth) {
        val track = tracks[trackName] ?: addTrack(trackName, interpolation)
        track.addKeyframe(time, value)
    }

    fun sample(trackName: String): Vec2 = tracks[trackName]?.sample(currentTime) ?: Vec2(0f, 0f)

    fun update(dt: Float) {
        if (!playing) return
        currentTime += dt

        val maxDuration = tracks.values.fold(0f) { acc, track -> max(acc, track.duration()) }
        if (loop && maxDuration > 0f && currentTime > maxDuration) {
            currentTime = 0f
        }
    }
}

class AnimatedRig(
    private val timeline: AnimationTimeline,
    private val trackName: String = "position"
) {
    fun currentPosition(): Vec2 = timeline.sample(trackName)

    fun addPositionKeyframe(time: Float, position: Vec2) {
        timeline.addKeyframe(trackName, time, position)
    }
}

class TimelineController(
    private val timeline: AnimationTimeline
) {
    fun play() {
        timeline.playing = true
    }

    fun stop() {
        timeline.playing = false
    }
}

class AnimationRigBuilder {
    fun create(): AnimationTimeline = AnimationTimeline()
}

class AnimationRigSession(
    private val timeline: AnimationTimeline
) {
    fun setLoopPose(start: Vec2, end: Vec2, duration: Float) {
        val track = timeline.addTrack("position")
        track.clear()
        track.addKeyframe(0f, start)
        track.addKeyframe(duration, end)
    }
}

class AnimationKeyframeLayer(
    private val timeline: AnimationTimeline,
    private val trackName: String = "position"
) {
    fun add(time: Float, value: Vec2) {
        timeline.addKeyframe(trackName, time, value)
    }
}

class AnimationBlendSystem(
    private val timeline: AnimationTimeline,
    private val weightPaint: WeightPaint,
    private val binder: MeshInfluenceBinder
) {
    fun evaluate(vertexIndex: Int, physicsPose: Vec2): Vec2 {
        val animationPose = timeline.sample("position")
        val strength = binder.effectiveInfluence(vertexIndex)
        return Vec2(
            animationPose.x * (1f - strength) + physicsPose.x * strength,
            animationPose.y * (1f - strength) + physicsPose.y * strength
        )
    }
}

class AnimationRigPipeline(
    private val timeline: AnimationTimeline,
    private val binder: MeshInfluenceBinder,
    private val weightPaint: WeightPaint
) {
    fun currentVertexPose(vertexIndex: Int, physicsPose: Vec2): Vec2 =
        binder.blend(timeline.sample("position"), physicsPose, 0.6f, vertexIndex)
}

class MeshAnimationBridge(
    private val timeline: AnimationTimeline,
    private val weightPaint: WeightPaint,
    private val binder: MeshInfluenceBinder
) {
    fun animatedPoseForVertex(vertexIndex: Int, physicsPose: Vec2): Vec2 =
        binder.blend(timeline.sample("position"), physicsPose, 0.6f, vertexIndex)
}

class AnimationRigStack(
    private val timeline: AnimationTimeline,
    private val weightPaint: WeightPaint,
    private val binder: MeshInfluenceBinder
) {
    fun poseAt(vertexIndex: Int, physicsPose: Vec2): Vec2 =
        binder.blend(timeline.sample("position"), physicsPose, 0.6f, vertexIndex)
}

class CombinedPoseSolver(
    private val timeline: AnimationTimeline,
    private val weightPaint: WeightPaint,
    private val binder: MeshInfluenceBinder
) {
    fun finalPose(vertexIndex: Int, physicsPose: Vec2): Vec2 =
        binder.blend(timeline.sample("position"), physicsPose, 0.6f, vertexIndex)
}

class WeightDrivenPoseBlender(
    private val timeline: AnimationTimeline,
    private val meshBinding: MeshBinding,
    private val weightPaint: WeightPaint
) {
    fun poseForVertex(vertexIndex: Int, physicsPose: Vec2): Vec2 {
        val animationPose = timeline.sample("position")
        val strength = weightPaint.weightFor(vertexIndex)
        return Vec2(
            animationPose.x * (1f - strength) + physicsPose.x * strength,
            animationPose.y * (1f - strength) + physicsPose.y * strength
        )
    }
}

class WeightDrivenBindingSystem(
    private val timeline: AnimationTimeline,
    private val weightPaint: WeightPaint,
    private val meshBinding: MeshBinding
) {
    fun update(transform: Vec2) {
        val animationPose = timeline.sample("position")
        val blend = meshBinding.blend(animationPose, transform, 0.6f)
        meshBinding.transform = blend
    }
}

class WeightPaintAwareRig(
    private val timeline: AnimationTimeline,
    private val weightPaint: WeightPaint,
    private val meshBinding: MeshBinding
) {
    fun applyToVertex(vertexIndex: Int, physicsPose: Vec2): Vec2 {
        val animationPose = timeline.sample("position")
        val weight = weightPaint.weightFor(vertexIndex)
        return Vec2(
            animationPose.x * (1f - weight) + physicsPose.x * weight,
            animationPose.y * (1f - weight) + physicsPose.y * weight
        )
    }
}
