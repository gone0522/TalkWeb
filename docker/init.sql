CREATE SCHEMA IF NOT EXISTS chatapp;

-- 用戶表
CREATE TABLE IF NOT EXISTS chatapp.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_data BYTEA,
    avatar_mime_type VARCHAR(50),
    avatar_default_icon INT DEFAULT 1,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 好友/關聯表
CREATE TABLE IF NOT EXISTS chatapp.friendships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    friend_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_friend UNIQUE (user_id, friend_id)
);

-- 群組表
CREATE TABLE IF NOT EXISTS chatapp.groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon INT DEFAULT 1,
    announcement TEXT,
    created_by BIGINT NOT NULL REFERENCES chatapp.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 群組成員表
CREATE TABLE IF NOT EXISTS chatapp.group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES chatapp.groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_user UNIQUE (group_id, user_id)
);

-- 訊息表
CREATE TABLE IF NOT EXISTS chatapp.messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    receiver_id BIGINT REFERENCES chatapp.users(id),
    group_id BIGINT REFERENCES chatapp.groups(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT, EMOJI, SYSTEM
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 訊息已讀狀態表
CREATE TABLE IF NOT EXISTS chatapp.message_read_status (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES chatapp.messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_message_user UNIQUE (message_id, user_id)
);

-- 索引優化
CREATE INDEX IF NOT EXISTS idx_messages_sender ON chatapp.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON chatapp.messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_group ON chatapp.messages(group_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON chatapp.messages(created_at);
CREATE INDEX IF NOT EXISTS idx_read_status_msg ON chatapp.message_read_status(message_id, user_id);

-- 初始預設管理員帳號 (admin / admin123)
-- BCrypt for 'admin123': $2a$10$oQQ97ROa9jgJ8Aaez7NmteIkGiyZWPCWwdoG6mI3vAuEDJGMcQ2wW
INSERT INTO chatapp.users (username, password_hash, nickname, is_admin, must_change_password, status)
VALUES ('admin', '$2a$10$oQQ97ROa9jgJ8Aaez7NmteIkGiyZWPCWwdoG6mI3vAuEDJGMcQ2wW', '系統管理員', TRUE, TRUE, 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

