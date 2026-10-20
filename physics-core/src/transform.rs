#[derive(Clone, Copy, Debug)]
pub struct Transform {
    pub position: crate::body::Vec2,
    pub rotation: f32,
    pub scale: crate::body::Vec2,
}

impl Transform {
    pub const fn identity() -> Self {
        Self {
            position: crate::body::Vec2::new(0.0, 0.0),
            rotation: 0.0,
            scale: crate::body::Vec2::new(1.0, 1.0),
        }
    }
}
