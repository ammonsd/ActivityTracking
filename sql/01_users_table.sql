-- Create Users table
CREATE TABLE dbo."Users" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    firstname  VARCHAR(50),
    lastname  VARCHAR(50) NOT NULL,
    company  VARCHAR(100),
    "password" VARCHAR(255) NOT NULL,
    "role" VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    forcepasswordupdate BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);

-- Create unique index on username to prevent duplicate usernames
-- This ensures data integrity and provides optimal query performance
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username_unique 
ON dbo."Users" (username);

-- Add foreign key constraint from TaskActivity to Users
-- This prevents deletion of users who have task activities
ALTER TABLE dbo."TaskActivity"
ADD CONSTRAINT FK_TaskActivity_Username
FOREIGN KEY ("username") REFERENCES dbo."Users"(username)
ON DELETE RESTRICT
ON UPDATE CASCADE;

