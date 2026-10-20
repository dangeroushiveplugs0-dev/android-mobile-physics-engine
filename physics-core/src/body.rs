#[derive(Clone, Copy, Debug)]
pub struct Vec2 {
    pub x: f32,
    pub y: f32,
}

impl Vec2 {
    pub const fn new(x: f32, y: f32) -> Self {
        Self { x, y }
    }

    pub fn dot(self, other: Self) -> f32 {
        self.x * other.x + self.y * other.y
    }

    pub fn length_sq(self) -> f32 {
        self.dot(self)
    }

    pub fn length(self) -> f32 {
        self.length_sq().sqrt()
    }

    pub fn normalized(self) -> Self {
        let len = self.length();
        if len > 0.0 {
            Self::new(self.x / len, self.y / len)
        } else {
            Self::new(0.0, 0.0)
        }
    }
}

impl std::ops::Add for Vec2 {
    type Output = Self;
    fn add(self, rhs: Self) -> Self::Output {
        Self::new(self.x + rhs.x, self.y + rhs.y)
    }
}

impl std::ops::Sub for Vec2 {
    type Output = Self;
    fn sub(self, rhs: Self) -> Self::Output {
        Self::new(self.x - rhs.x, self.y - rhs.y)
    }
}

impl std::ops::Mul<f32> for Vec2 {
    type Output = Self;
    fn mul(self, rhs: f32) -> Self::Output {
        Self::new(self.x * rhs, self.y * rhs)
    }
}

impl std::ops::Div<f32> for Vec2 {
    type Output = Self;
    fn div(self, rhs: f32) -> Self::Output {
        Self::new(self.x / rhs, self.y / rhs)
    }
}

impl std::ops::AddAssign for Vec2 {
    fn add_assign(&mut self, rhs: Self) {
        self.x += rhs.x;
        self.y += rhs.y;
    }
}

impl std::ops::SubAssign for Vec2 {
    fn sub_assign(&mut self, rhs: Self) {
        self.x -= rhs.x;
        self.y -= rhs.y;
    }
}

impl std::ops::MulAssign<f32> for Vec2 {
    fn mul_assign(&mut self, rhs: f32) {
        self.x *= rhs;
        self.y *= rhs;
    }
}

impl std::ops::DivAssign<f32> for Vec2 {
    fn div_assign(&mut self, rhs: f32) {
        self.x /= rhs;
        self.y /= rhs;
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum BodyType {
    Dynamic,
    Static,
    Kinematic,
}

#[derive(Clone, Debug)]
pub struct RigidBody {
    pub id: String,
    pub position: Vec2,
    pub velocity: Vec2,
    pub angular_velocity: f32,
    pub mass: f32,
    pub inv_mass: f32,
    pub restitution: f32,
    pub friction: f32,
    pub body_type: BodyType,
    pub shape: crate::shape::Shape,
}

impl RigidBody {
    pub fn new(id: impl Into<String>, position: Vec2, shape: crate::shape::Shape) -> Self {
        let mass = match shape {
            crate::shape::Shape::Circle { radius } => radius * radius * 0.12,
            crate::shape::Shape::Box { width, height } => width * height * 0.08,
            crate::shape::Shape::Mesh { .. } => 1.0,
        };

        Self {
            id: id.into(),
            position,
            velocity: Vec2::new(0.0, 0.0),
            angular_velocity: 0.0,
            mass,
            inv_mass: 1.0 / mass,
            restitution: 0.82,
            friction: 0.85,
            body_type: BodyType::Dynamic,
            shape,
        }
    }
}
