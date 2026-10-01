INSERT INTO users (id, login_id, password_hash, name, role, active) VALUES
(1, 'user1', '1234', 'Test User One', 'USER', TRUE),
(2, 'admin2', '1234', 'Test Admin Two', 'ADMIN', TRUE),
(3, 'user3', '1234', 'Test User Three', 'USER', TRUE),
(4, 'user4', '1234', 'Inactive User', 'USER', FALSE),
(5, 'admin5', '1234', 'Test Admin Five', 'ADMIN', TRUE);
INSERT INTO facilities (id, name, location, active) VALUES
(1, 'Server Room', 'A', TRUE),
(2, 'Archive', 'B', TRUE),
(3, 'Closed Lab', 'C', FALSE);
