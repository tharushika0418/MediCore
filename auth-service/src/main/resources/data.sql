-- ===================================================================
-- Auth Service - schema and seed data
-- -------------------------------------------------------------------
-- Runs on every startup (spring.sql.init.mode=always), so every
-- statement is idempotent: it never fails or duplicates data on a re-run.
-- ===================================================================

-- Ensure the 'users' table exists ("user" is a reserved word in PostgreSQL).
-- Columns match the User JPA entity.
CREATE TABLE IF NOT EXISTS users
(
    id       UUID PRIMARY KEY,
    email    VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255)        NOT NULL,
    role     VARCHAR(50)         NOT NULL
);

-- Test user for local development.
--   email:    testuser@test.com
--   password: password123   (stored below ONLY as a BCrypt hash)
-- Never seed real accounts or production passwords in this file.
INSERT INTO users (id, email, password, role)
SELECT '223e4567-e89b-12d3-a456-426614174006',
       'testuser@test.com',
       '$2a$10$0HrdKM2CvOlTTBY5PZYA.OMJlASJGwq.WP9oTaXbtyanndkffvMvW',
       'ADMIN'
WHERE NOT EXISTS (SELECT 1
                  FROM users
                  WHERE id = '223e4567-e89b-12d3-a456-426614174006'
                     OR email = 'testuser@test.com');
