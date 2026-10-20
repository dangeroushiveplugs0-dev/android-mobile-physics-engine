use crate::body::{BodyType, RigidBody, Vec2};
use crate::shape::Shape;

#[derive(Debug)]
pub struct World {
    pub width: f32,
    pub height: f32,
    pub gravity: Vec2,
    pub bodies: Vec<RigidBody>,
}

impl World {
    pub fn new(width: f32, height: f32) -> Self {
        Self {
            width,
            height,
            gravity: Vec2::new(0.0, 900.0),
            bodies: Vec::new(),
        }
    }

    pub fn add_body(&mut self, body: RigidBody) {
        self.bodies.push(body);
    }

    pub fn update(&mut self, dt: f32) {
        let fixed_dt = 1.0 / 120.0;
        let mut accumulator = dt;

        while accumulator > 0.0 {
            let step = f32::min(fixed_dt, accumulator);
            self.step(step);
            accumulator -= step;
        }
    }

    fn step(&mut self, dt: f32) {
        for body in &mut self.bodies {
            if body.body_type == BodyType::Static {
                continue;
            }
            body.velocity.y += self.gravity.y * dt;
            body.position.x += body.velocity.x * dt;
            body.position.y += body.velocity.y * dt;
            self.resolve_bounds(body);
        }

        self.resolve_collisions();
    }

    fn resolve_bounds(&self, body: &mut RigidBody) {
        let radius = match body.shape {
            Shape::Circle { radius } => radius,
            Shape::Box { width, height } => (width.max(height)) * 0.5,
            Shape::Mesh { ref vertices } => {
                let max_dim = vertices.iter().fold(0.0f32, |acc, v| acc.max(v.x.abs().max(v.y.abs())));
                max_dim
            }
        };

        if body.position.x < radius {
            body.position.x = radius;
            body.velocity.x = body.velocity.x.abs() * 0.78;
        }
        if body.position.x > self.width - radius {
            body.position.x = self.width - radius;
            body.velocity.x = -body.velocity.x.abs() * 0.78;
        }
        if body.position.y < radius {
            body.position.y = radius;
            body.velocity.y = body.velocity.y.abs() * 0.78;
        }
        if body.position.y > self.height - radius {
            body.position.y = self.height - radius;
            body.velocity.y = -body.velocity.y.abs() * 0.78;
            body.velocity.x *= 0.995;
        }
    }

    fn resolve_collisions(&mut self) {
        for i in 0..self.bodies.len() {
            for j in (i + 1)..self.bodies.len() {
                let a = &self.bodies[i];
                let b = &self.bodies[j];
                if a.body_type == BodyType::Static && b.body_type == BodyType::Static {
                    continue;
                }
                // Minimal collision response shell for a reusable toolkit.
                // More advanced collision logic can be layered on top later.
                let dx = b.position.x - a.position.x;
                let dy = b.position.y - a.position.y;
                let dist_sq = dx * dx + dy * dy;
                let min_dist = match (&a.shape, &b.shape) {
                    (Shape::Circle { radius: ra }, Shape::Circle { radius: rb }) => ra + rb,
                    (Shape::Box { width: wa, height: ha }, Shape::Box { width: wb, height: hb }) => ((wa.max(*ha) + wb.max(*hb)) * 0.5),
                    _ => 32.0,
                };

                if dist_sq < min_dist * min_dist {
                    let dist = dist_sq.sqrt();
                    let nx = if dist > 0.0 { dx / dist } else { 1.0 };
                    let ny = if dist > 0.0 { dy / dist } else { 0.0 };
                    let overlap = min_dist - dist;
                    let inv_mass_a = if a.body_type == BodyType::Static { 0.0 } else { a.inv_mass };
                    let inv_mass_b = if b.body_type == BodyType::Static { 0.0 } else { b.inv_mass };
                    let total_inv = inv_mass_a + inv_mass_b;
                    if total_inv > 0.0 {
                        let correction = overlap / total_inv;
                        let mut body_a = &mut self.bodies[i];
                        if body_a.body_type != BodyType::Static {
                            body_a.position.x -= nx * correction * inv_mass_a;
                            body_a.position.y -= ny * correction * inv_mass_a;
                        }
                        let mut body_b = &mut self.bodies[j];
                        if body_b.body_type != BodyType::Static {
                            body_b.position.x += nx * correction * inv_mass_b;
                            body_b.position.y += ny * correction * inv_mass_b;
                        }
                    }
                }
            }
        }
    }
}
