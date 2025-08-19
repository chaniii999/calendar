-- MySQL 8.x 스키마 정의
-- 데이터베이스는 미리 `CREATE DATABASE calendar;` 후 사용하세요.

SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- users 테이블
CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(26) PRIMARY KEY,
  email VARCHAR(40) NOT NULL UNIQUE,
  nickname VARCHAR(15) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- schedules 테이블
CREATE TABLE IF NOT EXISTS schedules (
  id VARCHAR(26) PRIMARY KEY,
  user_id VARCHAR(26) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NULL,
  color VARCHAR(7) NULL,
  schedule_date DATE NOT NULL,
  start_time TIME NULL,
  end_time TIME NULL,
  is_all_day BIT(1) NOT NULL,
  is_recurring BIT(1) NOT NULL,
  recurrence_rule VARCHAR(20) NULL,
  status VARCHAR(20) NOT NULL,
  completion_rate INT NULL,
  reminder_minutes INT NULL,
  is_reminder_enabled BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL,
  CONSTRAINT fk_schedules_users FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_id ON schedules(user_id);
CREATE INDEX idx_schedule_date ON schedules(schedule_date);
CREATE INDEX idx_created_at ON schedules(created_at);


