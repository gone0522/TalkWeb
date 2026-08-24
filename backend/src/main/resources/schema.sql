CREATE SCHEMA IF NOT EXISTS chatapp;

-- 1. 使用者主表 (users)
CREATE TABLE IF NOT EXISTS chatapp.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_data BYTEA,
    avatar_mime_type VARCHAR(50),
    avatar_default_icon INT DEFAULT 1,
    is_admin BOOLEAN DEFAULT FALSE,
    must_change_password BOOLEAN DEFAULT TRUE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. 群組主表 (groups)
CREATE TABLE IF NOT EXISTS chatapp.groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    announcement TEXT,
    avatar_default_icon INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. 群組成員表 (group_members)
CREATE TABLE IF NOT EXISTS chatapp.group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES chatapp.groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_user UNIQUE (group_id, user_id)
);

-- 4. 訊息主表 (messages)
CREATE TABLE IF NOT EXISTS chatapp.messages (
    id BIGSERIAL PRIMARY KEY,
    chat_type VARCHAR(20) NOT NULL,
    sender_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    receiver_id BIGINT REFERENCES chatapp.users(id),
    group_id BIGINT REFERENCES chatapp.groups(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    url_preview_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. 訊息已讀狀態表 (message_read_status)
CREATE TABLE IF NOT EXISTS chatapp.message_read_status (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES chatapp.messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id) ON DELETE CASCADE,
    read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_msg_user UNIQUE (message_id, user_id)
);

-- 6. 好友關聯表 (friendships)
CREATE TABLE IF NOT EXISTS chatapp.friendships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES chatapp.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_friend UNIQUE (user_id, friend_id)
);

-- 索引建立
CREATE INDEX IF NOT EXISTS idx_messages_sender ON chatapp.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON chatapp.messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_group ON chatapp.messages(group_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON chatapp.messages(created_at);
CREATE INDEX IF NOT EXISTS idx_group_members_user ON chatapp.group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_read_status_msg ON chatapp.message_read_status(message_id, user_id);
CREATE INDEX IF NOT EXISTS idx_friendships_user ON chatapp.friendships(user_id);
CREATE INDEX IF NOT EXISTS idx_friendships_friend ON chatapp.friendships(friend_id);

-- 初始預設管理員帳號 (admin / admin123)
INSERT INTO chatapp.users (username, password_hash, nickname, is_admin, must_change_password, status)
VALUES ('admin', '$2a$10$oQQ97ROa9jgJ8Aaez7NmteIkGiyZWPCWwdoG6mI3vAuEDJGMcQ2wW', '系統管理員', TRUE, TRUE, 'ACTIVE')
ON CONFLICT (username) DO NOTHING;
