-- 기초데이터(예시)
-- 주의: OAuth2 사용자 생성은 실제 로그인 플로우에서 이루어집니다.
-- 데모용 사용자 및 일정 샘플입니다.

INSERT INTO users (id, email, nickname, created_at, updated_at)
VALUES
  ('01JABCDETESTUSERULID00001', 'demo.user1@example.com', '데모1', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE email = VALUES(email);

INSERT INTO schedules (
  id, user_id, title, description, color, schedule_date,
  start_time, end_time, is_all_day, is_recurring, recurrence_rule,
  status, completion_rate, reminder_minutes, is_reminder_enabled,
  created_at, updated_at
) VALUES (
  '01JABCDETESTSCHDULID00001', '01JABCDETESTUSERULID00001',
  '캘린더 시작하기', '샘플 일정입니다.', '#FF5733', CURDATE(),
  '09:00:00', '10:00:00', b'0', b'0', NULL,
  'PLANNED', 0, 10, b'1', NOW(6), NOW(6)
) ON DUPLICATE KEY UPDATE title = VALUES(title);


