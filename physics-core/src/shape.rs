#[derive(Clone, Debug)]
pub enum Shape {
    Circle { radius: f32 },
    Box { width: f32, height: f32 },
    Mesh { vertices: Vec<Vec2> },
}

#[derive(Clone, Copy, Debug)]
pub struct Vec2 {
    pub x: f32,
    pub y: f32,
}

impl Vec2 {
    pub const fn new(x: f32, y: f32) -> Self {
        Self { x, y }
    }
}
