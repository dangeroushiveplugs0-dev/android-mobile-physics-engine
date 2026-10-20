use crate::body::Vec2;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum BindingMode {
    Full,
    Partial,
    PhysicsDriven,
}

#[derive(Clone, Debug)]
pub struct WeightVertex {
    pub vertex_index: usize,
    pub weight: f32,
}

#[derive(Clone, Debug)]
pub struct WeightPaint {
    pub vertex_weights: Vec<WeightVertex>,
}

impl WeightPaint {
    pub fn new() -> Self {
        Self {
            vertex_weights: Vec::new(),
        }
    }

    pub fn add(&mut self, vertex_index: usize, weight: f32) {
        self.vertex_weights.push(WeightVertex { vertex_index, weight });
    }

    pub fn normalized_weight(&self, vertex_index: usize) -> f32 {
        let found = self.vertex_weights.iter().find(|w| w.vertex_index == vertex_index);
        found.map(|w| w.weight).unwrap_or(0.0)
    }
}

#[derive(Clone, Debug)]
pub struct MeshBinding {
    pub binding_mode: BindingMode,
    pub weight_paint: WeightPaint,
    pub surface_mesh: Vec<Vec2>,
    pub collision_mesh: Vec<Vec2>,
    pub influence_name: String,
}

impl MeshBinding {
    pub fn new(surface_mesh: Vec<Vec2>, collision_mesh: Vec<Vec2>, influence_name: impl Into<String>) -> Self {
        Self {
            binding_mode: BindingMode::Partial,
            weight_paint: WeightPaint::new(),
            surface_mesh,
            collision_mesh,
            influence_name: influence_name.into(),
        }
    }

    pub fn set_weight(&mut self, vertex_index: usize, weight: f32) {
        self.weight_paint.add(vertex_index, weight);
    }

    pub fn blend_transform(&self, animation_position: Vec2, physics_position: Vec2, blend_strength: f32) -> Vec2 {
        Vec2::new(
            animation_position.x * (1.0 - blend_strength) + physics_position.x * blend_strength,
            animation_position.y * (1.0 - blend_strength) + physics_position.y * blend_strength,
        )
    }
}
