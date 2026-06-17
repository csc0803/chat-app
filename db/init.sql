-- ============================================================
-- Chat App — Database Initialization Script
-- ============================================================

CREATE DATABASE IF NOT EXISTS chatdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE chatdb;

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at    TIMESTAMP    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- rooms
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
  id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100)  NOT NULL,
  created_by  BIGINT        NOT NULL,
  created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
  deleted_at  TIMESTAMP     NULL,
  CONSTRAINT fk_rooms_user FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- room_members
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS room_members (
  room_id    BIGINT    NOT NULL,
  user_id    BIGINT    NOT NULL,
  is_admin   BOOLEAN   NOT NULL DEFAULT FALSE,
  joined_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted BOOLEAN   NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMP NULL,
  PRIMARY KEY (room_id, user_id),
  CONSTRAINT fk_rm_room FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_rm_user FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 規則 5 查詢用：找最早加入且仍在 room 的 user
CREATE INDEX idx_rm_room_joined ON room_members(room_id, joined_at ASC);

-- ------------------------------------------------------------
-- messages
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
  id       BIGINT    AUTO_INCREMENT PRIMARY KEY,
  room_id  BIGINT    NOT NULL,
  user_id  BIGINT    NOT NULL,
  content  TEXT      NOT NULL,
  sent_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分頁查詢複合索引：先過濾房間，再按時間倒序
CREATE INDEX idx_messages_room_sent ON messages(room_id, sent_at DESC);

-- ------------------------------------------------------------
-- Seed data（開發用，可刪）
-- ------------------------------------------------------------
INSERT IGNORE INTO users (username, password_hash) VALUES
  ('admin', '$2a$12$placeholder_bcrypt_hash_here');   -- 上線前替換

INSERT IGNORE INTO rooms (name, created_by) VALUES
  ('general', 1),
  ('random',  1);
