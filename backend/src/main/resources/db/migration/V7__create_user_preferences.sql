CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    favorite_profile_codes TEXT,
    favorite_color_codes TEXT,
    preferred_profile_order TEXT,
    preferred_color_order TEXT,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
