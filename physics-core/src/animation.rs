use crate::body::Vec2;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum InterpolationMode {
    Linear,
    Smooth,
    Hold,
}

#[derive(Clone, Copy, Debug)]
pub struct Keyframe {
    pub time: f32,
    pub value: Vec2,
}

#[derive(Clone, Debug)]
pub struct AnimationTrack {
    pub name: String,
    pub keys: Vec<Keyframe>,
    pub interpolation: InterpolationMode,
}

impl AnimationTrack {
    pub fn new(name: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            keys: Vec::new(),
            interpolation: InterpolationMode::Smooth,
        }
    }

    pub fn add_keyframe(&mut self, time: f32, value: Vec2) {
        self.keys.push(Keyframe { time, value });
        self.keys.sort_by(|a, b| a.time.partial_cmp(&b.time).unwrap_or(std::cmp::Ordering::Equal));
    }

    pub fn sample(&self, time: f32) -> Vec2 {
        if self.keys.is_empty() {
            return Vec2::new(0.0, 0.0);
        }
        if self.keys.len() == 1 {
            return self.keys[0].value;
        }

        let mut prev = &self.keys[0];
        let mut next = &self.keys[0];

        for key in &self.keys {
            if key.time <= time {
                prev = key;
            }
            if key.time >= time {
                next = key;
                break;
            }
        }

        if prev.time == next.time {
            return prev.value;
        }

        let t = (time - prev.time) / (next.time - prev.time);
        let value = match self.interpolation {
            InterpolationMode::Linear => {
                Vec2::new(
                    prev.value.x + (next.value.x - prev.value.x) * t,
                    prev.value.y + (next.value.y - prev.value.y) * t,
                )
            }
            InterpolationMode::Smooth => {
                let smooth_t = t * t * (3.0 - 2.0 * t);
                Vec2::new(
                    prev.value.x + (next.value.x - prev.value.x) * smooth_t,
                    prev.value.y + (next.value.y - prev.value.y) * smooth_t,
                )
            }
            InterpolationMode::Hold => prev.value,
        };

        value
    }
}

#[derive(Clone, Debug)]
pub struct Pose {
    pub position: Vec2,
    pub rotation: f32,
    pub scale: Vec2,
}

#[derive(Clone, Debug)]
pub struct Animator {
    pub tracks: Vec<AnimationTrack>,
    pub time: f32,
    pub playing: bool,
    pub looped: bool,
}

impl Animator {
    pub fn new() -> Self {
        Self {
            tracks: Vec::new(),
            time: 0.0,
            playing: false,
            looped: true,
        }
    }

    pub fn add_track(&mut self, track: AnimationTrack) {
        self.tracks.push(track);
    }

    pub fn update(&mut self, dt: f32) {
        if !self.playing {
            return;
        }

        self.time += dt;

        let max_time = self.tracks.iter().flat_map(|track| &track.keys).map(|k| k.time).fold(0.0f32, f32::max);
        if self.looped && max_time > 0.0 && self.time > max_time {
            self.time = 0.0;
        }
    }

    pub fn sample_pose(&self) -> Pose {
        let mut position = Vec2::new(0.0, 0.0);
        let mut rotation = 0.0f32;
        let mut scale = Vec2::new(1.0, 1.0);

        for track in &self.tracks {
            let sampled = track.sample(self.time);
            if track.name == "position" {
                position = sampled;
            } else if track.name == "rotation" {
                rotation = sampled.x;
            } else if track.name == "scale" {
                scale = sampled;
            }
        }

        Pose {
            position,
            rotation,
            scale,
        }
    }
}
